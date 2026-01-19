package cod.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cod.command.ICommand;
import cod.database.Database;
import cod.model.Developer;
import cod.model.Milestone;
import cod.model.Ticket;
import cod.model.TicketAction;
import cod.model.User;
import cod.model.enums.Seniority;
import cod.model.enums.TicketType;
import cod.utils.NotificationManager;

import java.util.ArrayList;
import java.util.List;

public class AssignTicketCommand implements ICommand {
    private final JsonNode args;
    private final ObjectMapper mapper = new ObjectMapper();

    public AssignTicketCommand(JsonNode args) {
        this.args = args;
    }

    @Override
    public ObjectNode execute() {
        Database db = Database.getInstance();
        ObjectNode result = mapper.createObjectNode();

        String username = args.has("username") ? args.get("username").asText() : "";
        String timestamp = args.has("timestamp") ? args.get("timestamp").asText() : "";
        int ticketId = args.has("ticketID") ? args.get("ticketID").asInt() : -1;

        NotificationManager.checkDeadlines(db, timestamp);
        NotificationManager.checkUnblocking(db, timestamp);

        User user = db.getUser(username);
        if (user == null) {
            return buildError(result, "assignTicket", username, timestamp, "The user " + username + " does not exist.");
        }
        if (!"DEVELOPER".equals(user.getRole())) {
            return buildError(result, "assignTicket", username, timestamp,
                    "The user does not have permission to execute this command: required role DEVELOPER; user role " + user.getRole() + ".");
        }
        if (db.isTestingPhase()) {
            return buildError(result, "assignTicket", username, timestamp, "Tickets can only be assigned during development phase.");
        }

        Ticket t = db.getTicket(ticketId);
        if (t == null) {
            return buildError(result, "assignTicket", username, timestamp, "Ticket not found.");
        }
        if (!"OPEN".equals(t.getStatus())) {
            return buildError(result, "assignTicket", username, timestamp, "Only OPEN tickets can be assigned.");
        }

        Milestone milestone = null;
        for (Milestone m : db.getMilestones()) {
            if (m.getTickets().contains(ticketId)) {
                milestone = m;
                break;
            }
        }
        if (milestone == null) {
            return buildError(result, "assignTicket", username, timestamp, "Ticket is not part of any milestone.");
        }
        if (!milestone.getAssignedDevs().contains(username)) {
            return buildError(result, "assignTicket", username, timestamp, "Developer " + username + " is not assigned to milestone " + milestone.getName() + ".");
        }
        if (milestone.getIsBlocked()) {
            return buildError(result, "assignTicket", username, timestamp, "Cannot assign ticket " + ticketId + " from blocked milestone " + milestone.getName() + ".");
        }

        Developer dev = (Developer) user;
        String expError = checkExpertise(dev.getExpertiseArea(), t.getExpertiseArea(), username, ticketId);
        if (expError != null) return buildError(result, "assignTicket", username, timestamp, expError);
        String senError = checkSeniority(dev.getSeniority(), t, username, ticketId);
        if (senError != null) return buildError(result, "assignTicket", username, timestamp, senError);

        t.setAssignedTo(username);
        t.setAssignedAt(timestamp);

        TicketAction assignAction = new TicketAction();
        assignAction.setAction("ASSIGNED");
        assignAction.setBy(username);
        assignAction.setTimestamp(timestamp);
        t.addHistory(assignAction);

        TicketAction statusAction = new TicketAction();
        statusAction.setAction("STATUS_CHANGED");
        statusAction.setBy(username);
        statusAction.setTimestamp(timestamp);
        statusAction.setFrom("OPEN");
        statusAction.setTo("IN_PROGRESS");
        t.addHistory(statusAction);

        t.setStatus("IN_PROGRESS");

        result.put("status", "success");
        return result;
    }

    private String checkExpertise(String devExp, String ticketExp, String username, int ticketId) {
        List<String> required = new ArrayList<>();
        if ("BACKEND".equals(ticketExp)) { required.add("BACKEND"); required.add("FULLSTACK"); }
        else if ("FRONTEND".equals(ticketExp)) { required.add("DESIGN"); required.add("FRONTEND"); required.add("FULLSTACK"); }
        else if ("DESIGN".equals(ticketExp)) { required.add("DESIGN"); required.add("FRONTEND"); required.add("FULLSTACK"); }
        else if ("DEVOPS".equals(ticketExp)) { required.add("DEVOPS"); required.add("FULLSTACK"); }
        else if ("DB".equals(ticketExp)) { required.add("BACKEND"); required.add("DB"); required.add("FULLSTACK"); }
        if (required.contains(devExp)) return null;
        required.sort(String::compareTo);
        return "Developer " + username + " cannot assign ticket " + ticketId + " due to expertise area. Required: " + String.join(", ", required) + "; Current: " + devExp + ".";
    }

    private String checkSeniority(Seniority seniority, Ticket t, String username, int ticketId) {
        String priority = t.getBusinessPriority();
        TicketType type = t.getType();
        List<String> required = new ArrayList<>();
        boolean canJunior = true;
        if ("HIGH".equals(priority) || "CRITICAL".equals(priority)) canJunior = false;
        if (type == TicketType.FEATURE_REQUEST) canJunior = false;
        boolean canMid = true;
        if ("CRITICAL".equals(priority)) canMid = false;
        boolean canSenior = true;
        if (canJunior) required.add("JUNIOR");
        if (canMid) required.add("MID");
        if (canSenior) required.add("SENIOR");
        if (required.contains(seniority.name())) return null;
        return "Developer " + username + " cannot assign ticket " + ticketId + " due to seniority level. Required: " + String.join(", ", required) + "; Current: " + seniority.name() + ".";
    }

    private ObjectNode buildError(ObjectNode result, String command, String username, String timestamp, String msg) {
        result.put("command", command);
        result.put("username", username);
        result.put("timestamp", timestamp);
        result.put("error", msg);
        return result;
    }
}