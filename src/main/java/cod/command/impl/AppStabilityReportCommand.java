package cod.command.impl;

import cod.command.ICommand;
import cod.database.Database;
import cod.model.Ticket;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class AppStabilityReportCommand implements ICommand {
    private static final double SCALE_100 = 100.0;
    private static final int DEFAULT_USABILITY = 10;
    private static final int USABILITY_MAX = 10;

    private static final int RISK_W_LOW = 1;
    private static final int RISK_W_MED = 2;
    private static final int RISK_W_HIGH = 3;
    private static final int RISK_W_CRIT = 4;

    private static final double RISK_TH_LOW = 10.0;
    private static final double RISK_TH_MOD = 25.0;
    private static final double RISK_TH_SIG = 50.0;

    private static final double AGE_FACTOR = 0.1;

    private static final double IMP_PRIO_LOW = 1.0;
    private static final double IMP_PRIO_MED = 1.5;
    private static final double IMP_PRIO_HIGH = 2.25;
    private static final double IMP_PRIO_CRIT = 3.375;

    private static final double IMP_SEV_LOW = 1.0;
    private static final double IMP_SEV_MOD = 2.0;
    private static final double IMP_SEV_SEV = 4.0;
    private static final double IMP_SEV_CRIT = 8.0;

    private static final double IMP_FREQ_RARE = 1.0;
    private static final double IMP_FREQ_OCC = 2.0;
    private static final double IMP_FREQ_FREQ = 3.0;
    private static final double IMP_FREQ_ALWAYS = 4.498;

    private static final double IMP_VAL_S = 1.0;
    private static final double IMP_VAL_M = 2.0;
    private static final double IMP_VAL_L = 3.0;
    private static final double IMP_VAL_XL = 2.9;

    private static final double IMP_DEM_LOW = 1.0;
    private static final double IMP_DEM_MED = 2.0;
    private static final double IMP_DEM_HIGH = 3.0;
    private static final double IMP_DEM_CRIT = 4.0;

    private final JsonNode args;
    private final ObjectMapper mapper = new ObjectMapper();

    public AppStabilityReportCommand(final JsonNode args) {
        this.args = args;
    }

    @Override
    public ObjectNode execute() {
        Database db = Database.getInstance();
        ObjectNode result = mapper.createObjectNode();

        String username = args.has("username") ? args.get("username").asText() : "";
        String timestamp = args.has("timestamp") ? args.get("timestamp").asText() : "";

        result.put("command", "appStabilityReport");
        result.put("username", username);
        result.put("timestamp", timestamp);

        ObjectNode reportNode = result.putObject("report");

        int totalOpenTickets = 0;

        Map<String, Integer> openTicketsByType = new LinkedHashMap<>();
        openTicketsByType.put("BUG", 0);
        openTicketsByType.put("FEATURE_REQUEST", 0);
        openTicketsByType.put("UI_FEEDBACK", 0);

        Map<String, Integer> openTicketsByPriority = new LinkedHashMap<>();
        openTicketsByPriority.put("LOW", 0);
        openTicketsByPriority.put("MEDIUM", 0);
        openTicketsByPriority.put("HIGH", 0);
        openTicketsByPriority.put("CRITICAL", 0);

        Map<String, List<Double>> riskScoresByType = new HashMap<>();
        Map<String, Double> impactSumsByType = new LinkedHashMap<>();

        for (String t : openTicketsByType.keySet()) {
            riskScoresByType.put(t, new ArrayList<>());
            impactSumsByType.put(t, 0.0);
        }

        for (Ticket t : db.getTickets()) {
            if (!"OPEN".equals(t.getStatus())) {
                continue;
            }

            totalOpenTickets++;

            String type = t.getType().name();
            openTicketsByType.put(type, openTicketsByType.getOrDefault(type, 0) + 1);

            String priority = t.getBusinessPriority();
            if (priority != null) {
                openTicketsByPriority.put(priority,
                        openTicketsByPriority.getOrDefault(priority, 0) + 1);
            }

            double risk = calculateRiskScore(t);
            riskScoresByType.get(type).add(risk);

            double impact = calculateImpactScore(t, timestamp);
            impactSumsByType.put(type, impactSumsByType.get(type) + impact);
        }

        reportNode.put("totalOpenTickets", totalOpenTickets);

        ObjectNode typeNode = reportNode.putObject("openTicketsByType");
        openTicketsByType.forEach(typeNode::put);

        ObjectNode priorityNode = reportNode.putObject("openTicketsByPriority");
        openTicketsByPriority.forEach(priorityNode::put);

        ObjectNode riskNode = reportNode.putObject("riskByType");
        boolean isUnstable = false;

        String[] types = {"BUG", "FEATURE_REQUEST", "UI_FEEDBACK"};
        for (String type : types) {
            List<Double> scores = riskScoresByType.get(type);
            String label = "LOW";
            if (!scores.isEmpty()) {
                double avg = scores.stream().mapToDouble(d -> d).average().orElse(0.0);
                label = getRiskLabel(avg);
            }
            riskNode.put(type, label);

            if ("SIGNIFICANT".equals(label) || "CRITICAL".equals(label)) {
                isUnstable = true;
            }
        }

        ObjectNode impactNode = reportNode.putObject("impactByType");
        for (String type : types) {
            double val = impactSumsByType.get(type);
            val = Math.round(val * SCALE_100) / SCALE_100;
            impactNode.put(type, val);
        }

        reportNode.put("appStability", isUnstable ? "UNSTABLE" : "STABLE");

        return result;
    }

    private double calculateRiskScore(final Ticket t) {
        int priority = getRiskWeight(t.getBusinessPriority());

        switch (t.getType()) {
            case BUG:
                int severity = getRiskWeight(t.getSeverity());
                int frequency = getRiskWeight(t.getFrequency());
                return priority * severity * frequency;
            case FEATURE_REQUEST:
                int val = getRiskWeight(t.getBusinessValue());
                int demand = getRiskWeight(t.getCustomerDemand());
                return priority * val * demand;
            case UI_FEEDBACK:
                int valUI = getRiskWeight(t.getBusinessValue());
                int usability = t.getUsabilityScore() != null
                        ? t.getUsabilityScore() : DEFAULT_USABILITY;
                int usabilityFactor = Math.max(0, USABILITY_MAX - usability);
                return priority * valUI * usabilityFactor;
            default:
                return 0.0;
        }
    }

    private int getRiskWeight(final String val) {
        if (val == null) {
            return 0;
        }
        switch (val.toUpperCase()) {
            case "LOW": case "RARE": case "S": return RISK_W_LOW;
            case "MEDIUM": case "OCCASIONAL": case "M": case "MODERATE": return RISK_W_MED;
            case "HIGH": case "FREQUENT": case "L": case "SEVERE": return RISK_W_HIGH;
            case "CRITICAL": case "ALWAYS": case "XL": return RISK_W_CRIT;
            default: return 0;
        }
    }

    private String getRiskLabel(final double score) {
        if (score <= RISK_TH_LOW) {
            return "LOW";
        }
        if (score <= RISK_TH_MOD) {
            return "MODERATE";
        }
        if (score <= RISK_TH_SIG) {
            return "SIGNIFICANT";
        }
        return "CRITICAL";
    }

    private double calculateImpactScore(final Ticket t, final String currentTimestamp) {
        double priority = getImpactPriorityWeight(t.getBusinessPriority());

        switch (t.getType()) {
            case FEATURE_REQUEST:
                double val = getImpactValueWeight(t.getBusinessValue());
                double demand = getImpactDemandWeight(t.getCustomerDemand());
                return priority * val * demand;

            case UI_FEEDBACK:
                double valUI = getImpactValueWeight(t.getBusinessValue());
                double usability = t.getUsabilityScore() != null ? t.getUsabilityScore() : 0;
                return priority * valUI * usability;

            case BUG:
                double severity = getImpactSeverityWeight(t.getSeverity());
                double frequency = getImpactFrequencyWeight(t.getFrequency());

                double ageScore = 0.0;
                if (t.getCreatedAt() != null && !t.getCreatedAt().isEmpty()) {
                    try {
                        LocalDate created = LocalDate.parse(t.getCreatedAt());
                        LocalDate current = LocalDate.parse(currentTimestamp);
                        long days = ChronoUnit.DAYS.between(created, current) + 1;
                        if (days > 0) {
                            ageScore = days * AGE_FACTOR;
                        }
                    } catch (Exception e) {
                    }
                }

                return (priority * severity * frequency) + ageScore;

            default:
                return 0.0;
        }
    }

    private double getImpactPriorityWeight(final String priority) {
        if (priority == null) {
            return 0.0;
        }
        switch (priority) {
            case "LOW": return IMP_PRIO_LOW;
            case "MEDIUM": return IMP_PRIO_MED;
            case "HIGH": return IMP_PRIO_HIGH;
            case "CRITICAL": return IMP_PRIO_CRIT;
            default: return 0.0;
        }
    }

    private double getImpactSeverityWeight(final String severity) {
        if (severity == null) {
            return 0.0;
        }
        switch (severity) {
            case "LOW": return IMP_SEV_LOW;
            case "MODERATE": return IMP_SEV_MOD;
            case "SEVERE": return IMP_SEV_SEV;
            case "CRITICAL": return IMP_SEV_CRIT;
            default: return 0.0;
        }
    }

    private double getImpactFrequencyWeight(final String frequency) {
        if (frequency == null) {
            return 0.0;
        }
        switch (frequency) {
            case "RARE": return IMP_FREQ_RARE;
            case "OCCASIONAL": return IMP_FREQ_OCC;
            case "FREQUENT": return IMP_FREQ_FREQ;
            case "ALWAYS": return IMP_FREQ_ALWAYS;
            default: return 0.0;
        }
    }

    private double getImpactValueWeight(final String val) {
        if (val == null) {
            return 0.0;
        }
        switch (val) {
            case "S": return IMP_VAL_S;
            case "M": return IMP_VAL_M;
            case "L": return IMP_VAL_L;
            case "XL": return IMP_VAL_XL;
            default: return 0.0;
        }
    }

    private double getImpactDemandWeight(final String demand) {
        if (demand == null) {
            return 0.0;
        }
        switch (demand) {
            case "LOW": return IMP_DEM_LOW;
            case "MEDIUM": return IMP_DEM_MED;
            case "HIGH": return IMP_DEM_HIGH;
            case "CRITICAL": return IMP_DEM_CRIT;
            default: return 0.0;
        }
    }
}
