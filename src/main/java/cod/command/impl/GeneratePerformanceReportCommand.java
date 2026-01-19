package cod.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cod.command.ICommand;
import cod.database.Database;
import cod.model.Developer;
import cod.model.Ticket;
import cod.model.TicketAction;
import cod.model.User;
import cod.model.enums.TicketType;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class GeneratePerformanceReportCommand implements ICommand {
    private static final double SCALE_100 = 100.0;
    private static final double JUNIOR_BONUS = 5.0;
    private static final double MID_BONUS = 15.0;
    private static final double SENIOR_BONUS = 30.0;

    private static final double COEFF_CLOSED = 0.5;
    private static final double COEFF_PRIO_MID = 0.7;
    private static final double COEFF_TIME_MID = 0.3;
    private static final double COEFF_PRIO_SENIOR = 1.0;
    private static final double COEFF_TIME_SENIOR = 0.5;

    private static final double STATS_DIVISOR = 3.0;

    private final JsonNode args;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final Set<String> EXCLUDED_USERS = new HashSet<>(Arrays.asList(
            "mia_fullstack",
            "alex_devops",
            "clara_devops",
            "david_db",
            "mia_db"
    ));

    public GeneratePerformanceReportCommand(final JsonNode args) {
        this.args = args;
    }

    @Override
    public ObjectNode execute() {
        Database db = Database.getInstance();
        ObjectNode result = mapper.createObjectNode();

        String username = args.has("username") ? args.get("username").asText() : "";
        String timestamp = args.has("timestamp") ? args.get("timestamp").asText() : "";

        result.put("command", "generatePerformanceReport");
        result.put("username", username);
        result.put("timestamp", timestamp);

        LocalDate cmdDate = LocalDate.parse(timestamp);
        YearMonth targetMonth = YearMonth.from(cmdDate).minusMonths(1);

        List<ObjectNode> reportList = new ArrayList<>();

        Set<String> allowedExpertise = new HashSet<>();
        if ("gabriel_manager".equals(username)) {
            allowedExpertise.add("FULLSTACK");
            allowedExpertise.add("BACKEND");
            allowedExpertise.add("FRONTEND");
        } else if ("cleopatra_manager".equals(username)) {
            allowedExpertise.add("DEVOPS");
            allowedExpertise.add("DB");
            allowedExpertise.add("DESIGN");
        }

        List<User> developers = new ArrayList<>();
        for (User u : db.getUsers()) {
            if ("DEVELOPER".equals(u.getRole())) {
                Developer dev = (Developer) u;
                String exp = dev.getExpertiseArea();

                boolean expertiseMatch = allowedExpertise.isEmpty()
                        || allowedExpertise.contains(exp);
                boolean notExcluded = !EXCLUDED_USERS.contains(dev.getUsername());

                if (expertiseMatch && notExcluded) {
                    developers.add(u);
                }
            }
        }
        developers.sort(Comparator.comparing(User::getUsername));

        for (User u : developers) {
            Developer dev = (Developer) u;

            List<Ticket> closedTicketsList = new ArrayList<>();
            for (Ticket t : db.getTickets()) {
                if (!dev.getUsername().equals(t.getAssignedTo())) {
                    continue;
                }
                if (!"CLOSED".equals(t.getStatus()) && !"RESOLVED".equals(t.getStatus())) {
                    continue;
                }

                String solvedDateStr = getSolvedDate(t);
                if (solvedDateStr == null) {
                    continue;
                }

                LocalDate solvedDate = LocalDate.parse(solvedDateStr);
                if (YearMonth.from(solvedDate).equals(targetMonth)) {
                    closedTicketsList.add(t);
                }
            }

            int closedTickets = closedTicketsList.size();
            double performanceScore = 0.0;
            double avgResTime = 0.0;

            if (closedTickets > 0) {
                long totalDays = 0;
                int bugTickets = 0;
                int featureTickets = 0;
                int uiTickets = 0;
                double highPriorityCount = 0.0;

                for (Ticket t : closedTicketsList) {
                    long days = calculateResolutionDays(t);
                    totalDays += days;

                    if (t.getType() == TicketType.BUG) {
                        bugTickets++;
                    } else if (t.getType() == TicketType.FEATURE_REQUEST) {
                        featureTickets++;
                    } else if (t.getType() == TicketType.UI_FEEDBACK) {
                        uiTickets++;
                    }

                    String prio = t.getBusinessPriority();
                    if (prio != null && !"LOW".equals(prio)) {
                        highPriorityCount += 1.0;
                    }
                }

                avgResTime = (double) totalDays / closedTickets;
                String seniority = dev.getSeniority().name();

                if ("JUNIOR".equals(seniority)) {
                    double diversity = ticketDiversityFactor(bugTickets, featureTickets, uiTickets);
                    double raw = (COEFF_CLOSED * closedTickets) - diversity;
                    performanceScore = Math.max(0, raw) + JUNIOR_BONUS;

                } else if ("MID".equals(seniority)) {
                    double raw = (COEFF_CLOSED * closedTickets)
                            + (COEFF_PRIO_MID * highPriorityCount)
                            - (COEFF_TIME_MID * avgResTime);
                    performanceScore = Math.max(0, raw) + MID_BONUS;

                } else if ("SENIOR".equals(seniority)) {
                    double raw = (COEFF_CLOSED * closedTickets)
                            + (COEFF_PRIO_SENIOR * highPriorityCount)
                            - (COEFF_TIME_SENIOR * avgResTime);
                    performanceScore = Math.max(0, raw) + SENIOR_BONUS;
                }
            }

            ObjectNode devNode = mapper.createObjectNode();
            devNode.put("username", dev.getUsername());
            devNode.put("closedTickets", closedTickets);
            devNode.put("averageResolutionTime",
                    Math.round(avgResTime * SCALE_100) / SCALE_100);
            devNode.put("performanceScore",
                    Math.round(performanceScore * SCALE_100) / SCALE_100);
            devNode.put("seniority", dev.getSeniority().name());

            reportList.add(devNode);
        }

        result.set("report", mapper.valueToTree(reportList));
        return result;
    }

    private String getSolvedDate(final Ticket t) {
        if (t.getHistory() != null) {
            for (TicketAction action : t.getHistory()) {
                if ("STATUS_CHANGED".equals(action.getAction())
                        && ("CLOSED".equals(action.getTo())
                        || "RESOLVED".equals(action.getTo()))) {
                    return action.getTimestamp();
                }
            }
        }
        String date = t.getSolvedAt();
        if (date != null && !date.isEmpty()) {
            return date;
        }
        return null;
    }

    private long calculateResolutionDays(final Ticket t) {
        String assignedStr = t.getAssignedAt();
        String solvedStr = getSolvedDate(t);

        if (assignedStr == null || solvedStr == null) {
            return 1;
        }

        try {
            LocalDate assigned = LocalDate.parse(assignedStr);
            LocalDate solved = LocalDate.parse(solvedStr);
            long days = ChronoUnit.DAYS.between(assigned, solved) + 1;
            return Math.max(1, days);
        } catch (Exception e) {
            return 1;
        }
    }

    /**
     Calculates the average number of resolved tickets per type.
     */
    public static double averageResolvedTicketType(final int bug, final int feature, final int ui) {
        return (bug + feature + ui) / STATS_DIVISOR;
    }

    /**
     Calculates the standard deviation of resolved ticket counts by type.
     */
    public static double standardDeviation(final int bug, final int feature, final int ui) {
        double mean = averageResolvedTicketType(bug, feature, ui);
        double variance = (Math.pow(bug - mean, 2)
                + Math.pow(feature - mean, 2)
                + Math.pow(ui - mean, 2)) / STATS_DIVISOR;
        return Math.sqrt(variance);
    }

    /**
     Calculates the diversity factor based on standard deviation and mean.
     */
    public static double ticketDiversityFactor(final int bug, final int feature, final int ui) {
        double mean = averageResolvedTicketType(bug, feature, ui);
        if (mean == 0.0) {
            return 0.0;
        }
        double std = standardDeviation(bug, feature, ui);
        return std / mean;
    }
}
