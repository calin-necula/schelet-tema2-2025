package cod.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cod.command.ICommand;
import cod.database.Database;
import cod.model.Ticket;
import cod.model.enums.TicketType;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class ReportTicketCommand implements ICommand {
    private final JsonNode args;
    private final ObjectMapper mapper = new ObjectMapper();

    public ReportTicketCommand(JsonNode args) {
        this.args = args;
    }

    @Override
    public ObjectNode execute() {
        Database db = Database.getInstance();
        ObjectNode result = mapper.createObjectNode();
        String username = args.has("username") ? args.get("username").asText() : "";
        String timestampStr = args.has("timestamp") ? args.get("timestamp").asText() : LocalDate.now().toString();

        if (db.getUser(username) == null) {
            result.put("command", "reportTicket");
            result.put("username", username);
            result.put("timestamp", timestampStr);
            result.put("error", "The user " + username + " does not exist.");
            return result;
        }

        if (!"REPORTER".equals(db.getUser(username).getRole())) {
            result.put("command", "reportTicket");
            result.put("username", username);
            result.put("timestamp", timestampStr);
            result.put("error", "The user does not have permission to execute this command: required role REPORTER; user role " + db.getUser(username).getRole() + ".");
            return result;
        }

        if (db.getTestingPhaseStartDate() == null) {
            db.setTestingPhaseStartDate(LocalDate.parse(timestampStr));
        }
        long daysElapsed = ChronoUnit.DAYS.between(db.getTestingPhaseStartDate(), LocalDate.parse(timestampStr)) + 1;

        if (!db.isTestingPhase() || daysElapsed > 12) {
            result.put("command", "reportTicket");
            result.put("username", username);
            result.put("timestamp", timestampStr);
            result.put("error", "Tickets can only be reported during testing phases.");
            return result;
        }

        Ticket t = new Ticket();
        t.setCreatedAt(timestampStr);

        if (args.has("params")) {
            JsonNode params = args.get("params");

            if (params.has("type")) {
                t.setType(TicketType.valueOf(params.get("type").asText()));
            }

            if (params.has("reportedBy")) {
                t.setReportedBy(params.get("reportedBy").asText());
            } else {
                t.setReportedBy(username);
            }

            t.setTitle(params.has("title") ? params.get("title").asText() : "");
            t.setDescription(params.has("description") ? params.get("description").asText() : "");
            t.setExpertiseArea(params.has("expertiseArea") ? params.get("expertiseArea").asText() : "");

            if (params.has("businessPriority")) {
                t.setBusinessPriority(params.get("businessPriority").asText());
            }

            if (t.getReportedBy().isEmpty()) {
                if (t.getType() != TicketType.BUG) {
                    result.put("command", "reportTicket");
                    result.put("username", username);
                    result.put("timestamp", timestampStr);
                    result.put("error", "Anonymous reports are only allowed for tickets of type BUG.");
                    return result;
                }
                t.resetInitialPriority("LOW");
            }

            mapSpecificFields(t, params);
        }

        db.addTicket(t);

        result.put("status", "success");
        return result;
    }

    private void mapSpecificFields(Ticket t, JsonNode params) {
        if (params.has("severity")) t.setSeverity(params.get("severity").asText());
        if (params.has("expectedBehavior")) t.setExpectedBehavior(params.get("expectedBehavior").asText());
        if (params.has("actualBehavior")) t.setActualBehavior(params.get("actualBehavior").asText());
        if (params.has("frequency")) t.setFrequency(params.get("frequency").asText());
        if (params.has("environment")) t.setEnvironment(params.get("environment").asText());
        if (params.has("errorCode")) t.setErrorCode(params.get("errorCode").asInt());

        if (params.has("businessValue")) t.setBusinessValue(params.get("businessValue").asText());
        if (params.has("customerDemand")) t.setCustomerDemand(params.get("customerDemand").asText());

        if (params.has("uiElementId")) t.setUiElementId(params.get("uiElementId").asText());
        if (params.has("usabilityScore")) t.setUsabilityScore(params.get("usabilityScore").asInt());
        if (params.has("suggestedFix")) t.setSuggestedFix(params.get("suggestedFix").asText());
    }
}