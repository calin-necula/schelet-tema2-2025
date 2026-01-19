package cod.command.impl;

import cod.command.ICommand;
import cod.database.Database;
import cod.model.Ticket;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GenerateCustomerImpactReportCommand implements ICommand {
    private static final double AGE_FACTOR = 0.1;

    private static final double PRIO_LOW = 1.0;
    private static final double PRIO_MED = 1.5;
    private static final double PRIO_HIGH = 2.25;
    private static final double PRIO_CRIT = 3.37;

    private static final double SEV_LOW = 1.0;
    private static final double SEV_MOD = 2.0;
    private static final double SEV_SEV = 4.0;
    private static final double SEV_CRIT = 8.0;

    private static final double FREQ_RARE = 1.0;
    private static final double FREQ_OCC = 2.0;
    private static final double FREQ_FREQ = 3.0;
    private static final double FREQ_ALWAYS = 4.0;

    private static final double VAL_S = 1.0;
    private static final double VAL_M = 2.0;
    private static final double VAL_L = 3.0;
    private static final double VAL_XL = 4.0;

    private static final double DEM_LOW = 1.0;
    private static final double DEM_MED = 2.0;
    private static final double DEM_HIGH = 3.0;
    private static final double DEM_CRIT = 4.0;

    private final JsonNode args;
    private final ObjectMapper mapper = new ObjectMapper();

    public GenerateCustomerImpactReportCommand(final JsonNode args) {
        this.args = args;
    }

    @Override
    public ObjectNode execute() {
        Database db = Database.getInstance();
        ObjectNode result = mapper.createObjectNode();

        String username = args.has("username") ? args.get("username").asText() : "";
        String timestamp = args.has("timestamp") ? args.get("timestamp").asText() : "";

        result.put("command", "generateCustomerImpactReport");
        result.put("username", username);
        result.put("timestamp", timestamp);

        ObjectNode reportNode = result.putObject("report");

        int totalTickets = 0;
        Map<String, Integer> ticketsByType = new LinkedHashMap<>();
        ticketsByType.put("BUG", 0);
        ticketsByType.put("FEATURE_REQUEST", 0);
        ticketsByType.put("UI_FEEDBACK", 0);

        Map<String, Integer> ticketsByPriority = new LinkedHashMap<>();
        ticketsByPriority.put("LOW", 0);
        ticketsByPriority.put("MEDIUM", 0);
        ticketsByPriority.put("HIGH", 0);
        ticketsByPriority.put("CRITICAL", 0);

        Map<String, Double> customerImpactByType = new LinkedHashMap<>();
        customerImpactByType.put("BUG", 0.0);
        customerImpactByType.put("FEATURE_REQUEST", 0.0);
        customerImpactByType.put("UI_FEEDBACK", 0.0);

        for (Ticket t : db.getTickets()) {
            if (!"OPEN".equals(t.getStatus())) {
                continue;
            }
            totalTickets++;
            String type = t.getType().name();
            ticketsByType.put(type, ticketsByType.getOrDefault(type, 0) + 1);

            String priority = t.getBusinessPriority();
            if (priority != null) {
                ticketsByPriority.put(priority,
                        ticketsByPriority.getOrDefault(priority, 0) + 1);
            }

            double impact = calculateImpact(t, timestamp);
            customerImpactByType.put(type,
                    customerImpactByType.getOrDefault(type, 0.0) + impact);
        }

        reportNode.put("totalTickets", totalTickets);

        ObjectNode typeNode = reportNode.putObject("ticketsByType");
        ticketsByType.forEach(typeNode::put);

        ObjectNode priorityNode = reportNode.putObject("ticketsByPriority");
        ticketsByPriority.forEach(priorityNode::put);

        ObjectNode impactNode = reportNode.putObject("customerImpactByType");
        customerImpactByType.forEach(impactNode::put);

        return result;
    }

    private double calculateImpact(final Ticket t, final String currentTimestamp) {
        double priorityWeight = getPriorityWeight(t.getBusinessPriority());

        switch (t.getType()) {
            case FEATURE_REQUEST:
                double val = getBusinessValueWeight(t.getBusinessValue());
                double demand = getCustomerDemandWeight(t.getCustomerDemand());
                return priorityWeight * val * demand;

            case UI_FEEDBACK:
                double valUI = getBusinessValueWeight(t.getBusinessValue());
                double usability = t.getUsabilityScore() != null ? t.getUsabilityScore() : 0;
                return priorityWeight * valUI * usability;

            case BUG:
                double severity = getSeverityWeight(t.getSeverity());
                double frequency = getFrequencyWeight(t.getFrequency());

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

                return (priorityWeight * severity * frequency) + ageScore;

            default:
                return 0.0;
        }
    }


    private double getPriorityWeight(final String priority) {
        if (priority == null) {
            return 0.0;
        }
        switch (priority) {
            case "LOW": return PRIO_LOW;
            case "MEDIUM": return PRIO_MED;
            case "HIGH": return PRIO_HIGH;
            case "CRITICAL": return PRIO_CRIT;
            default: return 0.0;
        }
    }

    private double getSeverityWeight(final String severity) {
        if (severity == null) {
            return 0.0;
        }
        switch (severity) {
            case "LOW": return SEV_LOW;
            case "MODERATE": return SEV_MOD;
            case "SEVERE": return SEV_SEV;
            case "CRITICAL": return SEV_CRIT;
            default: return 0.0;
        }
    }

    private double getFrequencyWeight(final String frequency) {
        if (frequency == null) {
            return 0.0;
        }
        switch (frequency) {
            case "RARE": return FREQ_RARE;
            case "OCCASIONAL": return FREQ_OCC;
            case "FREQUENT": return FREQ_FREQ;
            case "ALWAYS": return FREQ_ALWAYS;
            default: return 0.0;
        }
    }

    private double getBusinessValueWeight(final String val) {
        if (val == null) {
            return 0.0;
        }
        switch (val) {
            case "S": return VAL_S;
            case "M": return VAL_M;
            case "L": return VAL_L;
            case "XL": return VAL_XL;
            default: return 0.0;
        }
    }

    private double getCustomerDemandWeight(final String demand) {
        if (demand == null) {
            return 0.0;
        }
        switch (demand) {
            case "LOW": return DEM_LOW;
            case "MEDIUM": return DEM_MED;
            case "HIGH": return DEM_HIGH;
            case "CRITICAL": return DEM_CRIT;
            default: return 0.0;
        }
    }
}
