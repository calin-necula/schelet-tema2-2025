package cod.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cod.command.ICommand;
import cod.database.Database;
import cod.model.Comment;
import cod.model.Ticket;
import cod.model.User;

import java.util.List;

public final class UndoAddCommentCommand implements ICommand {
    private final JsonNode args;
    private final ObjectMapper mapper = new ObjectMapper();

    public UndoAddCommentCommand(final JsonNode args) {
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
            if (t.getReportedBy().isEmpty()) {
                result.put("command", "undoAddComment");
                result.put("username", username);
                result.put("timestamp", timestamp);
                result.put("error", "Comments are not allowed on anonymous tickets.");
                return result;
            }

            List<Comment> comments = t.getComments();
            for (int i = comments.size() - 1; i >= 0; i--) {
                if (comments.get(i).getAuthor().equals(username)) {
                    comments.remove(i);
                    break;
                }
            }
        }

        result.put("status", "success");
        return result;
    }
}
