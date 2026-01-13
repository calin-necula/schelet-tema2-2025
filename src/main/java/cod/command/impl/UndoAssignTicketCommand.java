package cod.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cod.command.ICommand;
import cod.database.Database;
import cod.model.Ticket;
import cod.model.TicketAction;
import cod.model.User;

public class UndoAssignTicketCommand implements ICommand {
    private final JsonNode args;
    private final ObjectMapper mapper = new ObjectMapper();

    public UndoAssignTicketCommand(JsonNode args) {
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

        if (user != null && t != null && username.equals(t.getAssignedTo())) {
            TicketAction action = new TicketAction();
            action.setAction("DE-ASSIGNED");
            action.setBy(username);
            action.setTimestamp(timestamp);
            t.addHistory(action);

            t.setAssignedTo("");
            t.setAssignedAt("");
            t.setStatus("OPEN");
        }

        result.put("status", "success");
        return result;
    }
}