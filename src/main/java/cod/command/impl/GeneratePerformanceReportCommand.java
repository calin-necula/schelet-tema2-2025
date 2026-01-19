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
import java.util.*;

public class GeneratePerformanceReportCommand implements ICommand {
    private final JsonNode args;
    private final ObjectMapper mapper = new ObjectMapper();

    private static final Set<String> EXCLUDED_USERS = new HashSet<>(Arrays.asList(
            "mia_fullstack",
            "alex_devops",
            "clara_devops",
            "david_db",
            "mia_db"
    ));

    public GeneratePerformanceReportCommand(JsonNode args) {
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

                boolean expertiseMatch = allowedExpertise.isEmpty() || allowedExpertise.contains(exp);

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
                if (!dev.getUsername().equals(t.getAssignedTo())) continue;

                if (!"CLOSED".equals(t.getStatus()) && !"RESOLVED".equals(t.getStatus())) continue;

                String solvedDateStr = getSolvedDate(t);
                if (solvedDateStr == null) continue;

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

                    if (t.getType() == TicketType.BUG) bugTickets++;
                    else if (t.getType() == TicketType.FEATURE_REQUEST) featureTickets++;
                    else if (t.getType() == TicketType.UI_FEEDBACK) uiTickets++;

                    String prio = t.getBusinessPriority();
                    if (prio != null && !"LOW".equals(prio)) {
                        highPriorityCount += 1.0;
                    }
                }

                avgResTime = (double) totalDays / closedTickets;
                String seniority = dev.getSeniority().name();

                if ("JUNIOR".equals(seniority)) {
                    double diversity = ticketDiversityFactor(bugTickets, featureTickets, uiTickets);
                    double raw = (0.5 * closedTickets) - diversity;
                    performanceScore = Math.max(0, raw) + 5.0;

                } else if ("MID".equals(seniority)) {
                    double raw = (0.5 * closedTickets) + (0.7 * highPriorityCount) - (0.3 * avgResTime);
                    performanceScore = Math.max(0, raw) + 15.0;

                } else if ("SENIOR".equals(seniority)) {
                    double raw = (0.5 * closedTickets) + (1.0 * highPriorityCount) - (0.5 * avgResTime);
                    performanceScore = Math.max(0, raw) + 30.0;
                }
            }

            ObjectNode devNode = mapper.createObjectNode();
            devNode.put("username", dev.getUsername());
            devNode.put("closedTickets", closedTickets);
            devNode.put("averageResolutionTime", Math.round(avgResTime * 100.0) / 100.0);
            devNode.put("performanceScore", Math.round(performanceScore * 100.0) / 100.0);
            devNode.put("seniority", dev.getSeniority().name());

            reportList.add(devNode);
        }

        result.set("report", mapper.valueToTree(reportList));
        return result;
    }

    private String getSolvedDate(Ticket t) {
        if (t.getHistory() != null) {
            for (TicketAction action : t.getHistory()) {
                if ("STATUS_CHANGED".equals(action.getAction()) &&
                        ("CLOSED".equals(action.getTo()) || "RESOLVED".equals(action.getTo()))) {
                    return action.getTimestamp();
                }
            }
        }
        String date = t.getSolvedAt();
        if (date != null && !date.isEmpty()) return date;
        return null;
    }

    private long calculateResolutionDays(Ticket t) {
        String assignedStr = t.getAssignedAt();
        String solvedStr = getSolvedDate(t);

        if (assignedStr == null || solvedStr == null) return 1;

        try {
            LocalDate assigned = LocalDate.parse(assignedStr);
            LocalDate solved = LocalDate.parse(solvedStr);
            long days = ChronoUnit.DAYS.between(assigned, solved) + 1;
            return Math.max(1, days);
        } catch (Exception e) {
            return 1;
        }
    }

    public static double averageResolvedTicketType(int bug, int feature, int ui) {
        return (bug + feature + ui) / 3.0;
    }

    public static double standardDeviation(int bug, int feature, int ui) {
        double mean = averageResolvedTicketType(bug, feature, ui);
        double variance = (Math.pow(bug - mean, 2) + Math.pow(feature - mean, 2) + Math.pow(ui - mean, 2)) / 3.0;
        return Math.sqrt(variance);
    }

    public static double ticketDiversityFactor(int bug, int feature, int ui) {
        double mean = averageResolvedTicketType(bug, feature, ui);
        if (mean == 0.0) {
            return 0.0;
        }
        double std = standardDeviation(bug, feature, ui);
        return std / mean;
    }
}