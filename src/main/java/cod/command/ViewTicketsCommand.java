package cod.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cod.command.ICommand;
import cod.database.Database;
import cod.model.Milestone;
import cod.model.Ticket;
import cod.model.User;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class ViewTicketsCommand implements ICommand {
    private final JsonNode args;
    private final ObjectMapper mapper = new ObjectMapper();

    public ViewTicketsCommand(JsonNode args) {
        this.args = args;
    }

    @Override
    public ObjectNode execute() {
        Database db = Database.getInstance();
        ObjectNode result = mapper.createObjectNode();
        String username = args.has("username") ? args.get("username").asText() : "";
        String timestamp = args.has("timestamp") ? args.get("timestamp").asText() : "";

        User user = db.getUser(username);
        if (user == null) {
            result.put("command", "viewTickets");
            result.put("username", username);
            result.put("timestamp", timestamp);
            result.put("error", "The user " + username + " does not exist.");
            return result;
        }

        updatePriorities(db, timestamp);

        result.put("command", "viewTickets");
        result.put("username", username);
        result.put("timestamp", timestamp);

        ArrayNode ticketsArray = result.putArray("tickets");
        List<Ticket> visibleTickets = new ArrayList<>();

        for (Ticket t : db.getTickets()) {
            boolean include = false;
            String role = user.getRole();

            if ("MANAGER".equals(role)) {
                include = true;
            } else if ("REPORTER".equals(role)) {
                if (t.getReportedBy().equals(username)) include = true;
            } else if ("DEVELOPER".equals(role)) {
                if ("OPEN".equals(t.getStatus())) {
                    for (Milestone m : db.getMilestones()) {
                        if (m.getAssignedDevs().contains(username) && m.getTickets().contains(t.getId())) {
                            include = true;
                            break;
                        }
                    }
                }
            }

            if (include) {
                visibleTickets.add(t);
            }
        }


        for (Ticket t : visibleTickets) {
            ticketsArray.add(mapper.valueToTree(t));
        }

        return result;
    }

    private void updatePriorities(Database db, String currentTimestampStr) {
        if (currentTimestampStr == null || currentTimestampStr.isEmpty()) return;
        LocalDate current = LocalDate.parse(currentTimestampStr);

        for (Milestone m : db.getMilestones()) {
            if (m.getIsBlocked()) continue;

            LocalDate created = LocalDate.parse(m.getCreatedAt());
            LocalDate due = LocalDate.parse(m.getDueDate());

            long daysActive = ChronoUnit.DAYS.between(created, current) + 1;
            int boost = (int) (daysActive / 3);

            boolean criticalMode = false;
            long daysToDue = ChronoUnit.DAYS.between(current, due);

            if (daysToDue == 1 || daysToDue < 0) criticalMode = true;

            for (Integer tId : m.getTickets()) {
                Ticket t = db.getTicket(tId);
                if (t == null) continue;
                if ("CLOSED".equals(t.getStatus())) continue;

                String initial = t.getInitialBusinessPriority();
                if (initial == null) initial = t.getBusinessPriority();

                int prioVal = getPriorityValue(initial);
                prioVal += boost;
                if (prioVal > 3) prioVal = 3;

                if (criticalMode) prioVal = 3;


                t.setComputedPriority(getPriorityString(prioVal));
            }
        }
    }

    private int getPriorityValue(String p) {
        if ("LOW".equals(p)) return 0;
        if ("MEDIUM".equals(p)) return 1;
        if ("HIGH".equals(p)) return 2;
        if ("CRITICAL".equals(p)) return 3;
        return 0;
    }

    private String getPriorityString(int v) {
        if (v <= 0) return "LOW";
        if (v == 1) return "MEDIUM";
        if (v == 2) return "HIGH";
        return "CRITICAL";
    }
}