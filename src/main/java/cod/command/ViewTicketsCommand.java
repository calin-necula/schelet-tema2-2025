package cod.command;

import cod.database.Database;
import cod.model.Milestone;
import cod.model.Ticket;
import cod.model.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

public final class ViewTicketsCommand implements ICommand {
    private static final int DAYS_PER_BOOST = 3;
    private static final int MAX_PRIORITY_VAL = 3;

    private static final int PRIO_LOW = 0;
    private static final int PRIO_MED = 1;
    private static final int PRIO_HIGH = 2;
    private static final int PRIO_CRIT = 3;

    private final JsonNode args;
    private final ObjectMapper mapper = new ObjectMapper();

    public ViewTicketsCommand(final JsonNode args) {
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
            result.put("command", "viewTickets");
            result.put("username", username);
            result.put("timestamp", timestamp);
            result.put("error", "The user " + username + " does not exist.");
            return result;
        }

        updatePriorities(db, timestamp);

        result.put("command", "viewTickets");
        result.put("username", username);
        result.put("timestamp", timestamp);

        ArrayNode ticketsArray = result.putArray("tickets");
        List<Ticket> visibleTickets = new ArrayList<>();

        for (Ticket t : db.getTickets()) {
            boolean include = false;
            String role = user.getRole();

            if ("MANAGER".equals(role)) {
                include = true;
            } else if ("REPORTER".equals(role)) {
                if (t.getReportedBy().equals(username)) {
                    include = true;
                }
            } else if ("DEVELOPER".equals(role)) {
                if ("OPEN".equals(t.getStatus())) {
                    for (Milestone m : db.getMilestones()) {
                        if (m.getAssignedDevs().contains(username)
                                && m.getTickets().contains(t.getId())) {
                            include = true;
                            break;
                        }
                    }
                }
            }

            if (include) {
                visibleTickets.add(t);
            }
        }

        for (Ticket t : visibleTickets) {
            ticketsArray.add(mapper.valueToTree(t));
        }

        return result;
    }

    private void updatePriorities(final Database db, final String currentTimestampStr) {
        if (currentTimestampStr == null || currentTimestampStr.isEmpty()) {
            return;
        }
        LocalDate current = LocalDate.parse(currentTimestampStr);

        for (Milestone m : db.getMilestones()) {
            if (m.getIsBlocked()) {
                continue;
            }

            LocalDate created = LocalDate.parse(m.getCreatedAt());
            LocalDate due = LocalDate.parse(m.getDueDate());

            long daysActive = ChronoUnit.DAYS.between(created, current) + 1;
            int boost = (int) (daysActive / DAYS_PER_BOOST);

            boolean criticalMode = false;
            long daysToDue = ChronoUnit.DAYS.between(current, due);

            if (daysToDue == 1 || daysToDue < 0) {
                criticalMode = true;
            }

            for (Integer tId : m.getTickets()) {
                Ticket t = db.getTicket(tId);
                if (t == null) {
                    continue;
                }
                if ("CLOSED".equals(t.getStatus())) {
                    continue;
                }

                String initial = t.getInitialBusinessPriority();
                if (initial == null) {
                    initial = t.getBusinessPriority();
                }

                int prioVal = getPriorityValue(initial);
                prioVal += boost;
                if (prioVal > MAX_PRIORITY_VAL) {
                    prioVal = MAX_PRIORITY_VAL;
                }

                if (criticalMode) {
                    prioVal = MAX_PRIORITY_VAL;
                }

                t.setComputedPriority(getPriorityString(prioVal));
            }
        }
    }

    private int getPriorityValue(final String p) {
        if ("LOW".equals(p)) {
            return PRIO_LOW;
        }
        if ("MEDIUM".equals(p)) {
            return PRIO_MED;
        }
        if ("HIGH".equals(p)) {
            return PRIO_HIGH;
        }
        if ("CRITICAL".equals(p)) {
            return PRIO_CRIT;
        }
        return 0;
    }

    private String getPriorityString(final int v) {
        if (v <= PRIO_LOW) {
            return "LOW";
        }
        if (v == PRIO_MED) {
            return "MEDIUM";
        }
        if (v == PRIO_HIGH) {
            return "HIGH";
        }
        return "CRITICAL";
    }
}
