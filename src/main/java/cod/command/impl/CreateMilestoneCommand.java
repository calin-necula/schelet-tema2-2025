package cod.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cod.command.ICommand;
import cod.database.Database;
import cod.model.Milestone;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public class CreateMilestoneCommand implements ICommand {
    private final JsonNode args;
    private final ObjectMapper mapper = new ObjectMapper();

    public CreateMilestoneCommand(JsonNode args) {
        this.args = args;
    }

    @Override
    public ObjectNode execute() {
        Database db = Database.getInstance();
        ObjectNode result = mapper.createObjectNode();

        String username = args.has("username") ? args.get("username").asText() : "";
        String timestampStr = args.has("timestamp") ? args.get("timestamp").asText() : "";

        if (db.isTestingPhase() && db.getTestingPhaseStartDate() != null && !timestampStr.isEmpty()) {
            try {
                LocalDate start = db.getTestingPhaseStartDate();
                LocalDate current = LocalDate.parse(timestampStr);
                long daysElapsed = ChronoUnit.DAYS.between(start, current) + 1;

                if (daysElapsed > 12) {
                    db.setTestingPhase(false);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (db.getUser(username) == null) {
            return buildError(result, "createMilestone", username, timestampStr, "Output eroare user inexistent");
        }

        if (!"MANAGER".equals(db.getUser(username).getRole())) {
            return buildError(result, "createMilestone", username, timestampStr,
                    "The user does not have permission to execute this command: required role MANAGER; user role " + db.getUser(username).getRole() + ".");
        }

        if (db.isTestingPhase()) {
            return buildError(result, "createMilestone", username, timestampStr,
                    "Milestones can be created only during development phase.");
        }

        List<Integer> newTickets = new ArrayList<>();
        if (args.has("tickets")) {
            for (JsonNode tNode : args.get("tickets")) newTickets.add(tNode.asInt());
        }

        for (Milestone m : db.getMilestones()) {
            for (Integer tId : newTickets) {
                if (m.getTickets().contains(tId)) {
                    return buildError(result, "createMilestone", username, timestampStr,
                            "Tickets " + tId + " already assigned to milestone " + m.getName() + ".");
                }
            }
        }

        Milestone m = new Milestone();
        m.setName(args.get("name").asText());
        m.setDueDate(args.get("dueDate").asText());
        m.setCreatedBy(username);
        m.setCreatedAt(timestampStr);
        m.setTickets(newTickets);

        List<String> devs = new ArrayList<>();
        if (args.has("assignedDevs")) {
            for (JsonNode dNode : args.get("assignedDevs")) devs.add(dNode.asText());
        }
        m.setAssignedDevs(devs);

        List<String> blocking = new ArrayList<>();
        if (args.has("blockingFor")) {
            for (JsonNode bNode : args.get("blockingFor")) blocking.add(bNode.asText());
        }
        m.setBlockingFor(blocking);

        db.addMilestone(m);

        result.put("status", "success");
        return result;
    }

    private ObjectNode buildError(ObjectNode result, String command, String username, String timestamp, String errorMsg) {
        result.put("command", command);
        result.put("username", username);
        result.put("timestamp", timestamp);
        result.put("error", errorMsg);
        return result;
    }
}