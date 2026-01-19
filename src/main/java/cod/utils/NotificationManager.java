package cod.utils;

import cod.database.Database;
import cod.model.Milestone;
import cod.model.Ticket;
import cod.model.User;

import java.time.LocalDate;
import java.util.List;

public class NotificationManager {

    public static void notifyUser(String username, String message) {
        Database db = Database.getInstance();
        User user = db.getUser(username);
        if (user != null) {
            user.addNotification(message);
        }
    }

    public static void notifyUsers(List<String> usernames, String message) {
        for (String u : usernames) {
            notifyUser(u, message);
        }
    }

    public static void checkDeadlines(Database db, String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) return;
        LocalDate current = LocalDate.parse(timestamp);

        for (Milestone m : db.getMilestones()) {
            if (m.isNotifiedDueTomorrow()) continue;

            if (m.getIsBlocked()) continue;

            LocalDate due = LocalDate.parse(m.getDueDate());
            LocalDate reminderDate = due.minusDays(1);

            if (!current.isBefore(reminderDate) && current.isBefore(due)) {
                m.setNotifiedDueTomorrow(true);
                String msg = "Milestone " + m.getName() + " is due tomorrow. All unresolved tickets are now CRITICAL.";

                notifyUsers(m.getAssignedDevs(), msg);
                updateTicketsToCritical(db, m);
            }
        }
    }

    public static void checkUnblocking(Database db, String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) return;
        LocalDate current = LocalDate.parse(timestamp);

        for (Milestone m : db.getMilestones()) {
            if (m.hasDependencies()) {
                boolean isBlocked = m.getIsBlocked();

                if (!isBlocked && !m.isUnblockedNotified()) {
                    m.setUnblockedNotified(true);

                    LocalDate due = LocalDate.parse(m.getDueDate());
                    if (current.isAfter(due)) {
                        String msg = "Milestone " + m.getName() + " was unblocked after due date. All active tickets are now CRITICAL.";
                        notifyUsers(m.getAssignedDevs(), msg);
                        updateTicketsToCritical(db, m);
                    }
                }
            }
        }
    }

    private static void updateTicketsToCritical(Database db, Milestone m) {
        for (Integer tId : m.getTickets()) {
            Ticket t = db.getTicket(tId);
            if (t != null && !"CLOSED".equals(t.getStatus())) {
                t.setBusinessPriority("CRITICAL");
            }
        }
    }
}