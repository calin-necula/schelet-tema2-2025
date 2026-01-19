package cod.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cod.command.ICommand;
import cod.database.Database;
import cod.model.User;
import cod.utils.NotificationManager;

import java.util.ArrayList;
import java.util.List;

public class ViewNotificationsCommand implements ICommand {
    private final JsonNode args;
    private final ObjectMapper mapper = new ObjectMapper();

    public ViewNotificationsCommand(JsonNode args) {
        this.args = args;
    }

    @Override
    public ObjectNode execute() {
        Database db = Database.getInstance();
        ObjectNode result = mapper.createObjectNode();
        String username = args.has("username") ? args.get("username").asText() : "";
        String timestamp = args.has("timestamp") ? args.get("timestamp").asText() : "";

        result.put("command", "viewNotifications");
        result.put("username", username);
        result.put("timestamp", timestamp);

        NotificationManager.checkDeadlines(db, timestamp);
        NotificationManager.checkUnblocking(db, timestamp);

        User user = db.getUser(username);
        ArrayNode notifsArray = result.putArray("notifications");

        if (user != null) {
            List<String> userNotifs = new ArrayList<>(user.getNotifications());
            for (String n : userNotifs) {
                notifsArray.add(n);
            }
            user.clearNotifications();
        }

        return result;
    }
}