package cod.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cod.command.ICommand;
import cod.database.Database;
import cod.model.Ticket;

public class StartTestingPhaseCommand implements ICommand {
    private final String username;
    private final ObjectMapper mapper = new ObjectMapper();

    public StartTestingPhaseCommand(JsonNode args) {
        this.username = args.has("username") ? args.get("username").asText() : "";
    }

    @Override
    public ObjectNode execute() {
        Database db = Database.getInstance();
        ObjectNode result = mapper.createObjectNode();

        if (db.getUser(username) == null) {
            result.put("status", "error");
            result.put("message", "Output eroare user inexistent");
            return result;
        }

        if (!"MANAGER".equals(db.getUser(username).getRole())) {
            result.put("status", "error");
            result.put("message", "The user does not have permission to execute this command: required role MANAGER; user role " + db.getUser(username).getRole() + ".");
            return result;
        }

        boolean hasActiveMilestones = db.getMilestones().stream().anyMatch(m ->
                m.getTickets().stream().map(id -> db.getTickets().get(id))
                        .anyMatch(t -> !"CLOSED".equals(t.getStatus()) && !"RESOLVED".equals(t.getStatus()))
        );

        if (hasActiveMilestones) {
            result.put("status", "error");
            result.put("message", "Output eroare milestone active");
            return result;
        }

        db.setTestingPhase(true);
        result.put("status", "success");
        return result;
    }
}