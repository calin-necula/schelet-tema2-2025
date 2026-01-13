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

import java.util.List;

public class UndoChangeStatusTicketCommand implements ICommand {
    private final JsonNode args;
    private final ObjectMapper mapper = new ObjectMapper();

    public UndoChangeStatusTicketCommand(JsonNode args) {
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
                result.put("command", "undoChangeStatus");
                result.put("username", username);
                result.put("timestamp", timestamp);
                result.put("error", "Ticket " + ticketId + " is not assigned to developer " + username + ".");
                return result;
            }

            List<TicketAction> history = t.getHistory();
            TicketAction lastStatusChange = null;

            for (int i = history.size() - 1; i >= 0; i--) {
                if ("STATUS_CHANGED".equals(history.get(i).getAction())) {
                    lastStatusChange = history.get(i);
                    break;
                }
            }

            if (lastStatusChange != null) {
                String previousStatus = lastStatusChange.getFrom();
                String currentStatus = t.getStatus();

                TicketAction revertAction = new TicketAction();
                revertAction.setAction("STATUS_CHANGED");
                revertAction.setBy(username);
                revertAction.setTimestamp(timestamp);
                revertAction.setFrom(currentStatus);
                revertAction.setTo(previousStatus);

                t.addHistory(revertAction);
                t.setStatus(previousStatus);

                if ("CLOSED".equals(currentStatus) && !"CLOSED".equals(previousStatus)) {
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