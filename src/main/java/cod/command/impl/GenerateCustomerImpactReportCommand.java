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
import java.util.LinkedHashMap;
import java.util.Map;

public class GenerateCustomerImpactReportCommand implements ICommand {
    private final JsonNode args;
    private final ObjectMapper mapper = new ObjectMapper();

    public GenerateCustomerImpactReportCommand(JsonNode args) {
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
                ticketsByPriority.put(priority, ticketsByPriority.getOrDefault(priority, 0) + 1);
            }

            double impact = calculateImpact(t, timestamp);
            customerImpactByType.put(type, customerImpactByType.getOrDefault(type, 0.0) + impact);
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

    private double calculateImpact(Ticket t, String currentTimestamp) {
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
                        long days = ChronoUnit.DAYS.between(created, current) + 1; // Zile inclusive
                        if (days > 0) {
                            ageScore = days * 0.1;
                        }
                    } catch (Exception e) {
                    }
                }

                return (priorityWeight * severity * frequency) + ageScore;

            default:
                return 0.0;
        }
    }


    private double getPriorityWeight(String priority) {
        if (priority == null) return 0.0;
        switch (priority) {
            case "LOW": return 1.0;
            case "MEDIUM": return 1.5;
            case "HIGH": return 2.25;
            case "CRITICAL": return 3.37;
            default: return 0.0;
        }
    }

    private double getSeverityWeight(String severity) {
        if (severity == null) return 0.0;
        switch (severity) {
            case "LOW": return 1.0;
            case "MODERATE": return 2.0;
            case "SEVERE": return 4.0;
            case "CRITICAL": return 8.0;
            default: return 0.0;
        }
    }

    private double getFrequencyWeight(String frequency) {
        if (frequency == null) return 0.0;
        switch (frequency) {
            case "RARE": return 1.0;
            case "OCCASIONAL": return 2.0;
            case "FREQUENT": return 3.0;
            case "ALWAYS": return 4.0;
            default: return 0.0;
        }
    }

    private double getBusinessValueWeight(String val) {
        if (val == null) return 0.0;
        switch (val) {
            case "S": return 1.0;
            case "M": return 2.0;
            case "L": return 3.0;
            case "XL": return 4.0;
            default: return 0.0;
        }
    }

    private double getCustomerDemandWeight(String demand) {
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