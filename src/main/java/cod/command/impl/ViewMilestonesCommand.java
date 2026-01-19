package cod.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cod.command.ICommand;
import cod.database.Database;
import cod.model.Milestone;
import cod.model.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ViewMilestonesCommand implements ICommand {
    private final JsonNode args;
    private final ObjectMapper mapper = new ObjectMapper();

    public ViewMilestonesCommand(final JsonNode args) {
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
            result.put("command", "viewMilestones");
            result.put("username", username);
            result.put("timestamp", timestamp);
            result.put("error", "The user " + username + " does not exist.");
            return result;
        }

        result.put("command", "viewMilestones");
        result.put("username", username);
        result.put("timestamp", timestamp);
        ArrayNode milestonesArray = result.putArray("milestones");

        List<Milestone> visibleMilestones = new ArrayList<>();

        for (Milestone m : db.getMilestones()) {
            boolean show = false;
            String role = user.getRole();

            if ("MANAGER".equals(role)) {
                if (m.getCreatedBy().equals(username)) {
                    show = true;
                }
            } else if ("DEVELOPER".equals(role)) {
                if (m.getAssignedDevs().contains(username)) {
                    show = true;
                }
            }

            if (show) {
                m.calculateTimeFields(timestamp);
                visibleMilestones.add(m);
            }
        }

        visibleMilestones.sort(Comparator.comparing(Milestone::getDueDate)
                .thenComparing(Milestone::getName));

        for (Milestone m : visibleMilestones) {
            milestonesArray.add(mapper.valueToTree(m));
        }

        return result;
    }
}
