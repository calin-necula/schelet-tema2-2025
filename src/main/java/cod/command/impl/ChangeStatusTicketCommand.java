package cod.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cod.command.ICommand;
import cod.database.Database;
import cod.model.Milestone;
import cod.model.Ticket;
import cod.model.TicketAction;
import cod.model.User;


public class ChangeStatusTicketCommand implements ICommand {
    private final JsonNode args;
    private final ObjectMapper mapper = new ObjectMapper();

    public ChangeStatusTicketCommand(JsonNode args) {
        this.args = args;
    }

    @Override
    public ObjectNode execute() {
        Database db = Database.getInstance();
        ObjectNode result = mapper.createObjectNode();

        String username = args.has("username") ? args.get("username").asText() : "";
        String timestamp = args.has("timestamp") ? args.get("timestamp").asText() : "";
        int ticketId = args.has("ticketID") ? args.get("ticketID").asInt() : -1;

        User user = db.getUser(username);
        Ticket t = db.getTicket(ticketId);

        if (user != null && t != null) {
            if (!username.equals(t.getAssignedTo())) {
                result.put("command", "changeStatus");
                result.put("username", username);
                result.put("timestamp", timestamp);
                result.put("error", "Ticket " + ticketId + " is not assigned to developer " + username + ".");
                return result;
            }

            String currentStatus = t.getStatus();
            String newStatus = null;

            if ("IN_PROGRESS".equals(currentStatus)) {
                newStatus = "RESOLVED";
            } else if ("RESOLVED".equals(currentStatus)) {
                newStatus = "CLOSED";
            } else if ("OPEN".equals(currentStatus)) {
                newStatus = "IN_PROGRESS";
            }

            if (newStatus != null) {
                TicketAction action = new TicketAction();
                action.setAction("STATUS_CHANGED");
                action.setBy(username);
                action.setTimestamp(timestamp);
                action.setFrom(currentStatus);
                action.setTo(newStatus);

                t.addHistory(action);
                t.setStatus(newStatus);

                if ("CLOSED".equals(newStatus)) {
                    t.setSolvedAt(timestamp);
                } else {
                    t.setSolvedAt("");
                }

                updateMilestoneStatus(db, ticketId);
            }
        }

        result.put("status", "success");
        return result;
    }

    private void updateMilestoneStatus(Database db, int ticketId) {
        for (Milestone m : db.getMilestones()) {
            if (m.getTickets().contains(ticketId)) {
                boolean allClosed = true;
                for (Integer tId : m.getTickets()) {
                    Ticket t = db.getTicket(tId);
                    if (t == null || !"CLOSED".equals(t.getStatus())) {
                        allClosed = false;
                        break;
                    }
                }
                if (allClosed) {
                    m.setStatus("COMPLETED");
                } else {
                    m.setStatus("ACTIVE");
                }
            }
        }
    }
}