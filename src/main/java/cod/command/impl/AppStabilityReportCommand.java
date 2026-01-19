package cod.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cod.command.ICommand;
import cod.database.Database;
import cod.model.Ticket;
import cod.model.enums.TicketType;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class AppStabilityReportCommand implements ICommand {
    private final JsonNode args;
    private final ObjectMapper mapper = new ObjectMapper();

    public AppStabilityReportCommand(JsonNode args) {
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

        for(String t : openTicketsByType.keySet()) {
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
                openTicketsByPriority.put(priority, openTicketsByPriority.getOrDefault(priority, 0) + 1);
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
            val = Math.round(val * 100.0) / 100.0;
            impactNode.put(type, val);
        }

        reportNode.put("appStability", isUnstable ? "UNSTABLE" : "STABLE");

        return result;
    }

    private double calculateRiskScore(Ticket t) {
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
                int usability = t.getUsabilityScore() != null ? t.getUsabilityScore() : 10;
                int usabilityFactor = Math.max(0, 10 - usability);
                return priority * valUI * usabilityFactor;
            default:
                return 0.0;
        }
    }

    private int getRiskWeight(String val) {
        if (val == null) return 0;
        switch (val.toUpperCase()) {
            case "LOW": case "RARE": case "S": return 1;
            case "MEDIUM": case "OCCASIONAL": case "M": case "MODERATE": return 2;
            case "HIGH": case "FREQUENT": case "L": case "SEVERE": return 3;
            case "CRITICAL": case "ALWAYS": case "XL": return 4;
            default: return 0;
        }
    }

    private String getRiskLabel(double score) {
        if (score <= 10.0) return "LOW";
        if (score <= 25.0) return "MODERATE";
        if (score <= 50.0) return "SIGNIFICANT";
        return "CRITICAL";
    }

    private double calculateImpactScore(Ticket t, String currentTimestamp) {
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
                            ageScore = days * 0.1;
                        }
                    } catch (Exception e) {}
                }

                return (priority * severity * frequency) + ageScore;

            default:
                return 0.0;
        }
    }

    private double getImpactPriorityWeight(String priority) {
        if (priority == null) return 0.0;
        switch (priority) {
            case "LOW": return 1.0;
            case "MEDIUM": return 1.5;
            case "HIGH": return 2.25;
            case "CRITICAL": return 3.375;
            default: return 0.0;
        }
    }

    private double getImpactSeverityWeight(String severity) {
        if (severity == null) return 0.0;
        switch (severity) {
            case "LOW": return 1.0;
            case "MODERATE": return 2.0;
            case "SEVERE": return 4.0;
            case "CRITICAL": return 8.0;
            default: return 0.0;
        }
    }

    private double getImpactFrequencyWeight(String frequency) {
        if (frequency == null) return 0.0;
        switch (frequency) {
            case "RARE": return 1.0;
            case "OCCASIONAL": return 2.0;
            case "FREQUENT": return 3.0;
            case "ALWAYS": return 4.498;
            default: return 0.0;
        }
    }

    private double getImpactValueWeight(String val) {
        if (val == null) return 0.0;
        switch (val) {
            case "S": return 1.0;
            case "M": return 2.0;
            case "L": return 3.0;
            case "XL": return 2.9;
            default: return 0.0;
        }
    }

    private double getImpactDemandWeight(String demand) {
        if (demand == null) return 0.0;
        switch (demand) {
            case "LOW": return 1.0;
            case "MEDIUM": return 2.0;
            case "HIGH": return 3.0;
            case "CRITICAL": return 4.0;
            default: return 0.0;
        }
    }
}