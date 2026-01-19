package cod.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cod.command.ICommand;
import cod.database.Database;
import cod.model.Ticket;
import cod.model.TicketAction;
import cod.model.enums.TicketType;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GenerateResolutionEfficiencyReportCommand implements ICommand {
    private final JsonNode args;
    private final ObjectMapper mapper = new ObjectMapper();

    public GenerateResolutionEfficiencyReportCommand(JsonNode args) {
        this.args = args;
    }

    @Override
    public ObjectNode execute() {
        Database db = Database.getInstance();
        ObjectNode result = mapper.createObjectNode();

        String username = args.has("username") ? args.get("username").asText() : "";
        String timestamp = args.has("timestamp") ? args.get("timestamp").asText() : "";

        result.put("command", "generateResolutionEfficiencyReport");
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

        Map<String, List<Double>> efficiencyValuesByType = new LinkedHashMap<>();
        efficiencyValuesByType.put("BUG", new ArrayList<>());
        efficiencyValuesByType.put("FEATURE_REQUEST", new ArrayList<>());
        efficiencyValuesByType.put("UI_FEEDBACK", new ArrayList<>());

        for (Ticket t : db.getTickets()) {
            if (t.getAssignedTo() == null || t.getAssignedTo().isEmpty()) {
                continue;
            }

            totalTickets++;

            String type = t.getType().name();
            ticketsByType.put(type, ticketsByType.getOrDefault(type, 0) + 1);

            String priority = t.getBusinessPriority();
            if (priority != null) {
                ticketsByPriority.put(priority, ticketsByPriority.getOrDefault(priority, 0) + 1);
            }

            double efficiency = calculateEfficiency(t);
            if (efficiency >= 0) {
                efficiencyValuesByType.get(type).add(efficiency);
            }
        }

        reportNode.put("totalTickets", totalTickets);

        ObjectNode typeNode = reportNode.putObject("ticketsByType");
        ticketsByType.forEach(typeNode::put);

        ObjectNode priorityNode = reportNode.putObject("ticketsByPriority");
        ticketsByPriority.forEach(priorityNode::put);

        ObjectNode efficiencyNode = reportNode.putObject("efficiencyByType");
        for (String type : efficiencyValuesByType.keySet()) {
            List<Double> values = efficiencyValuesByType.get(type);
            double avg = 0.0;
            if (!values.isEmpty()) {
                avg = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            }
            avg = Math.round(avg * 100.0) / 100.0;
            efficiencyNode.put(type, avg);
        }

        return result;
    }

    private double calculateEfficiency(Ticket t) {
        if (t.getCreatedAt() == null || t.getCreatedAt().isEmpty()) return -1;
        String lastActionDate = t.getCreatedAt();
        if (t.getHistory() != null && !t.getHistory().isEmpty()) {
            TicketAction last = t.getHistory().get(t.getHistory().size() - 1);
            lastActionDate = last.getTimestamp();
        }

        long daysOpen = 0;
        try {
            LocalDate created = LocalDate.parse(t.getCreatedAt());
            LocalDate last = LocalDate.parse(lastActionDate);
            daysOpen = ChronoUnit.DAYS.between(created, last);
        } catch (Exception e) {
            return -1;
        }

        double denominator = Math.max(1, daysOpen - 10);

        double score = getMagicScore(t);

        return (score / denominator) * 100.0;
    }

    private double getMagicScore(Ticket t) {
        switch (t.getType()) {
            case BUG:
                // Severity + 1
                return getWeight(t.getSeverity()) + 1.0;
            case FEATURE_REQUEST:
                // CustomerDemand + 0.2
                return getWeight(t.getCustomerDemand()) + 0.2;
            case UI_FEEDBACK:
                // (10 - Usability) - 0.25
                int usability = t.getUsabilityScore() != null ? t.getUsabilityScore() : 10;
                return (10 - usability) - 0.25;
            default:
                return 0.0;
        }
    }

    private int getWeight(String val) {
        if (val == null) return 0;
        switch (val.toUpperCase()) {
            case "LOW": case "RARE": case "S": return 1;
            case "MEDIUM": case "OCCASIONAL": case "M": case "MODERATE": return 2;
            case "HIGH": case "FREQUENT": case "L": case "SEVERE": return 3;
            case "CRITICAL": case "ALWAYS": case "XL": return 4;
            default: return 0;
        }
    }
}