package cod.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cod.command.ICommand;
import cod.database.Database;
import cod.model.Ticket;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GenerateTicketRiskReportCommand implements ICommand {
    private static final int DEFAULT_USABILITY = 10;
    private static final int MAX_USABILITY = 10;

    private static final double RISK_TH_LOW = 10.0;
    private static final double RISK_TH_MOD = 25.0;
    private static final double RISK_TH_MAJ = 50.0;

    private static final int WEIGHT_LOW = 1;
    private static final int WEIGHT_MED = 2;
    private static final int WEIGHT_HIGH = 3;
    private static final int WEIGHT_CRIT = 4;

    private final JsonNode args;
    private final ObjectMapper mapper = new ObjectMapper();

    public GenerateTicketRiskReportCommand(final JsonNode args) {
        this.args = args;
    }

    @Override
    public ObjectNode execute() {
        Database db = Database.getInstance();
        ObjectNode result = mapper.createObjectNode();

        String username = args.has("username") ? args.get("username").asText() : "";
        String timestamp = args.has("timestamp") ? args.get("timestamp").asText() : "";

        result.put("command", "generateTicketRiskReport");
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

        Map<String, List<Double>> scoresByType = new LinkedHashMap<>();
        scoresByType.put("BUG", new ArrayList<>());
        scoresByType.put("FEATURE_REQUEST", new ArrayList<>());
        scoresByType.put("UI_FEEDBACK", new ArrayList<>());

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

            double score = calculateRiskScore(t);
            scoresByType.get(type).add(score);
        }

        reportNode.put("totalTickets", totalTickets);

        ObjectNode typeNode = reportNode.putObject("ticketsByType");
        ticketsByType.forEach(typeNode::put);

        ObjectNode priorityNode = reportNode.putObject("ticketsByPriority");
        ticketsByPriority.forEach(priorityNode::put);

        ObjectNode riskNode = reportNode.putObject("riskByType");
        for (String type : scoresByType.keySet()) {
            List<Double> scores = scoresByType.get(type);
            String label = "LOW";
            if (!scores.isEmpty()) {
                double avg = scores.stream().mapToDouble(Double::doubleValue)
                        .average().orElse(0.0);
                label = getRiskLabel(avg);
            }
            riskNode.put(type, label);
        }

        return result;
    }

    private double calculateRiskScore(final Ticket t) {
        int priority = getWeight(t.getBusinessPriority());

        switch (t.getType()) {
            case BUG:
                int severity = getWeight(t.getSeverity());
                int frequency = getWeight(t.getFrequency());
                return priority * severity * frequency;

            case FEATURE_REQUEST:
                int val = getWeight(t.getBusinessValue());
                int demand = getWeight(t.getCustomerDemand());
                return priority * val * demand;

            case UI_FEEDBACK:
                int valUI = getWeight(t.getBusinessValue());
                int usability = t.getUsabilityScore() != null
                        ? t.getUsabilityScore() : DEFAULT_USABILITY;
                int usabilityFactor = Math.max(0, MAX_USABILITY - usability);
                return priority * valUI * usabilityFactor;

            default:
                return 0.0;
        }
    }

    private String getRiskLabel(final double score) {
        if (score <= RISK_TH_LOW) {
            return "LOW";
        }
        if (score <= RISK_TH_MOD) {
            return "MODERATE";
        }
        if (score <= RISK_TH_MAJ) {
            return "MAJOR";
        }
        return "CRITICAL";
    }

    private int getWeight(final String val) {
        if (val == null) {
            return 0;
        }
        switch (val.toUpperCase()) {
            case "LOW": case "RARE": case "S": return WEIGHT_LOW;
            case "MEDIUM": case "OCCASIONAL": case "M": case "MODERATE": return WEIGHT_MED;
            case "HIGH": case "FREQUENT": case "L": case "SEVERE": return WEIGHT_HIGH;
            case "CRITICAL": case "ALWAYS": case "XL": return WEIGHT_CRIT;
            default: return 0;
        }
    }
}
