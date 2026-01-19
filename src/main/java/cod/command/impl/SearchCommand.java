package cod.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cod.command.ICommand;
import cod.database.Database;
import cod.model.Developer;
import cod.model.Milestone;
import cod.model.Ticket;
import cod.model.User;
import cod.model.enums.Seniority;
import cod.model.enums.TicketType;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SearchCommand implements ICommand {
    private final JsonNode args;
    private final ObjectMapper mapper = new ObjectMapper();

    public SearchCommand(final JsonNode args) {
        this.args = args;
    }

    @Override
    public ObjectNode execute() {
        Database db = Database.getInstance();
        ObjectNode result = mapper.createObjectNode();
        String username = args.has("username") ? args.get("username").asText() : "";
        String timestamp = args.has("timestamp") ? args.get("timestamp").asText() : "";

        result.put("command", "search");
        result.put("username", username);
        result.put("timestamp", timestamp);

        JsonNode filters = args.get("filters");
        String searchType = "";

        if (args.has("searchType")) {
            searchType = args.get("searchType").asText();
        } else if (filters != null && filters.has("searchType")) {
            searchType = filters.get("searchType").asText();
        }

        result.put("searchType", searchType);
        ArrayNode resultsArray = result.putArray("results");

        if ("DEVELOPER".equals(searchType)) {
            searchDevelopers(db, filters, resultsArray);
        } else if ("TICKET".equals(searchType)) {
            searchTickets(db, username, filters, resultsArray);
        }

        return result;
    }

    private void searchDevelopers(final Database db, final JsonNode filters,
                                  final ArrayNode resultsArray) {
        List<Developer> matchedDevs = new ArrayList<>();

        for (User user : db.getUsers()) {
            if (!"DEVELOPER".equals(user.getRole())) {
                continue;
            }

            Developer dev = (Developer) user;
            boolean match = true;

            if (filters != null) {
                if (filters.has("expertiseArea")
                        && !filters.get("expertiseArea").asText()
                        .equals(dev.getExpertiseArea())) {
                    match = false;
                }
                if (filters.has("seniority")
                        && !filters.get("seniority").asText()
                        .equals(dev.getSeniority().name())) {
                    match = false;
                }
            }

            if (match) {
                matchedDevs.add(dev);
            }
        }

        matchedDevs.sort(Comparator.comparing(Developer::getHireDate));

        for (Developer dev : matchedDevs) {
            ObjectNode devNode = mapper.valueToTree(dev);
            devNode.remove("email");
            devNode.remove("role");
            resultsArray.add(devNode);
        }
    }

    private void searchTickets(final Database db, final String username, final JsonNode filters,
                               final ArrayNode resultsArray) {
        User user = db.getUser(username);

        for (Ticket t : db.getTickets()) {
            if (!"OPEN".equals(t.getStatus())) {
                continue;
            }

            boolean match = true;
            List<String> matchedKeywords = new ArrayList<>();

            if (filters != null) {
                if (filters.has("type")
                        && !filters.get("type").asText().equals(t.getType().name())) {
                    match = false;
                }
                if (filters.has("businessPriority")
                        && !filters.get("businessPriority").asText()
                        .equals(t.getBusinessPriority())) {
                    match = false;
                }

                if (filters.has("createdAfter")) {
                    LocalDate created = LocalDate.parse(t.getCreatedAt());
                    LocalDate after = LocalDate.parse(filters.get("createdAfter").asText());
                    if (!created.isAfter(after)) {
                        match = false;
                    }
                }

                if (filters.has("availableForAssignment")
                        && filters.get("availableForAssignment").asBoolean()) {
                    if (user != null && "DEVELOPER".equals(user.getRole())) {
                        Developer dev = (Developer) user;
                        if (!isAvailableForAssignment(db, dev, t)) {
                            match = false;
                        }
                    } else {
                        match = false;
                    }
                }

                if (filters.has("keywords")) {
                    boolean keywordMatch = false;
                    String content = (t.getTitle() + " "
                            + (t.getDescription() != null ? t.getDescription() : "")).toLowerCase();
                    for (JsonNode kwNode : filters.get("keywords")) {
                        String kw = kwNode.asText();
                        if (content.contains(kw.toLowerCase())) {
                            keywordMatch = true;
                            matchedKeywords.add(kw);
                        }
                    }
                    if (!keywordMatch) {
                        match = false;
                    }
                }
            }

            if (match) {
                ObjectNode tNode = mapper.valueToTree(t);
                tNode.remove("assignedTo");
                tNode.remove("assignedAt");
                tNode.remove("comments");

                if (!matchedKeywords.isEmpty()) {
                    ArrayNode kwArray = tNode.putArray("matchingWords");
                    for (String kw : matchedKeywords) {
                        kwArray.add(kw);
                    }
                }

                resultsArray.add(tNode);
            }
        }
    }

    private boolean isAvailableForAssignment(final Database db, final Developer dev,
                                             final Ticket t) {
        if (t.getAssignedTo() != null && !t.getAssignedTo().isEmpty()) {
            return false;
        }

        boolean inUserMilestone = false;
        for (Milestone m : db.getMilestones()) {
            if (m.getTickets().contains(t.getId())
                    && m.getAssignedDevs().contains(dev.getUsername())) {
                inUserMilestone = true;
                break;
            }
        }
        if (!inUserMilestone) {
            return false;
        }

        if (!checkExpertise(dev.getExpertiseArea(), t.getExpertiseArea())) {
            return false;
        }
        if (!checkSeniority(dev.getSeniority(), t)) {
            return false;
        }

        return true;
    }

    private boolean checkExpertise(final String devExp, final String ticketExp) {
        if ("FULLSTACK".equals(devExp)) {
            return true;
        }

        if ("FRONTEND".equals(ticketExp) || "DESIGN".equals(ticketExp)) {
            return "FRONTEND".equals(devExp) || "DESIGN".equals(devExp);
        }
        if ("BACKEND".equals(ticketExp) || "DB".equals(ticketExp)) {
            return "BACKEND".equals(devExp) || "DB".equals(devExp);
        }
        if ("DEVOPS".equals(ticketExp)) {
            return "DEVOPS".equals(devExp);
        }

        return devExp.equals(ticketExp);
    }

    private boolean checkSeniority(final Seniority seniority, final Ticket t) {
        String priority = t.getBusinessPriority();
        TicketType type = t.getType();

        boolean canJunior = true;
        if ("HIGH".equals(priority) || "CRITICAL".equals(priority)) {
            canJunior = false;
        }
        if (type == TicketType.FEATURE_REQUEST) {
            canJunior = false;
        }

        boolean canMid = true;
        if ("CRITICAL".equals(priority)) {
            canMid = false;
        }

        if (seniority == Seniority.JUNIOR && !canJunior) {
            return false;
        }
        if (seniority == Seniority.MID && !canMid) {
            return false;
        }

        return true;
    }
}
