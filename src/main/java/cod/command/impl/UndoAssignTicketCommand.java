package cod.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cod.command.ICommand;
import cod.database.Database;
import cod.model.Ticket;
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
        int ticketId = args.has("ticketID") ? args.get("ticketID").asInt() : -1;

        User user = db.getUser(username);
        if (user == null) {
            return null;
        }

        Ticket t = db.getTicket(ticketId);
        if (t != null && username.equals(t.getAssignedTo())) {
            t.setAssignedTo("");
            t.setAssignedAt("");
            t.setStatus("OPEN");
        }

        result.put("status", "success");
        return result;
    }
}