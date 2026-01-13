package cod.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cod.command.ICommand;
import cod.database.Database;
import cod.model.Comment;
import cod.model.Ticket;
import cod.model.User;

public class AddCommentCommand implements ICommand {
    private final JsonNode args;
    private final ObjectMapper mapper = new ObjectMapper();

    public AddCommentCommand(JsonNode args) {
        this.args = args;
    }

    @Override
    public ObjectNode execute() {
        Database db = Database.getInstance();
        ObjectNode result = mapper.createObjectNode();

        String username = args.has("username") ? args.get("username").asText() : "";
        int ticketId = args.has("ticketID") ? args.get("ticketID").asInt() : -1;
        String commentText = args.has("comment") ? args.get("comment").asText() : "";
        String timestamp = args.has("timestamp") ? args.get("timestamp").asText() : "";

        User user = db.getUser(username);
        if (user == null) return null;

        Ticket t = db.getTicket(ticketId);
        if (t == null) return null;

        if (t.getReportedBy().isEmpty()) {
            return buildError(result, "addComment", username, timestamp, "Comments are not allowed on anonymous tickets.");
        }

        if (commentText == null || commentText.length() < 10) {
            return buildError(result, "addComment", username, timestamp, "Comment must be at least 10 characters long.");
        }

        String role = user.getRole();

        if ("DEVELOPER".equals(role)) {
            if (!username.equals(t.getAssignedTo())) {
                return buildError(result, "addComment", username, timestamp,
                        "Ticket " + ticketId + " is not assigned to the developer " + username + ".");
            }
        } else if ("REPORTER".equals(role)) {
            if ("CLOSED".equals(t.getStatus())) {
                return buildError(result, "addComment", username, timestamp, "Reporters cannot comment on CLOSED tickets.");
            }

            if (!username.equals(t.getReportedBy())) {
                return buildError(result, "addComment", username, timestamp,
                        "Reporter " + username + " cannot comment on ticket " + ticketId + ".");
            }
        }

        Comment c = new Comment(username, commentText, timestamp);
        t.addComment(c);

        result.put("status", "success");
        return result;
    }

    private ObjectNode buildError(ObjectNode result, String command, String username, String timestamp, String msg) {
        result.put("command", command);
        result.put("username", username);
        result.put("timestamp", timestamp);
        result.put("error", msg);
        return result;
    }
}