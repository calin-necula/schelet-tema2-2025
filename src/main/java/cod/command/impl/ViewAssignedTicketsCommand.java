package cod.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cod.command.ICommand;
import cod.database.Database;
import cod.model.Ticket;
import cod.model.User;

import java.util.List;
import java.util.stream.Collectors;

public final class ViewAssignedTicketsCommand implements ICommand {
    private static final int PRIORITY_LOW = 0;
    private static final int PRIORITY_MEDIUM = 1;
    private static final int PRIORITY_HIGH = 2;
    private static final int PRIORITY_CRITICAL = 3;

    private final JsonNode args;
    private final ObjectMapper mapper = new ObjectMapper();

    public ViewAssignedTicketsCommand(final JsonNode args) {
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
            result.put("command", "viewAssignedTickets");
            result.put("username", username);
            result.put("timestamp", timestamp);
            result.put("error", "The user " + username + " does not exist.");
            return result;
        }

        result.put("command", "viewAssignedTickets");
        result.put("username", username);
        result.put("timestamp", timestamp);
        ArrayNode arr = result.putArray("assignedTickets");

        List<Ticket> assigned = db.getTickets().stream()
                .filter(t -> username.equals(t.getAssignedTo()))
                .collect(Collectors.toList());

        assigned.sort((t1, t2) -> {
            int p1 = getPriorityValue(t1.getBusinessPriority());
            int p2 = getPriorityValue(t2.getBusinessPriority());
            if (p1 != p2) {
                return Integer.compare(p2, p1);
            }
            return Integer.compare(t1.getId(), t2.getId());
        });

        for (Ticket t : assigned) {
            ObjectNode ticketNode = mapper.valueToTree(t);
            ticketNode.remove("assignedTo");
            ticketNode.remove("solvedAt");
            arr.add(ticketNode);
        }

        return result;
    }

    private int getPriorityValue(final String p) {
        if ("LOW".equals(p)) {
            return PRIORITY_LOW;
        }
        if ("MEDIUM".equals(p)) {
            return PRIORITY_MEDIUM;
        }
        if ("HIGH".equals(p)) {
            return PRIORITY_HIGH;
        }
        if ("CRITICAL".equals(p)) {
            return PRIORITY_CRITICAL;
        }
        return 0;
    }
}
