package cod.utils;

import cod.database.Database;
import cod.model.Milestone;
import cod.model.Ticket;
import cod.model.User;

import java.time.LocalDate;
import java.util.List;

/**
 Utility class for managing and sending notifications to users.
 */
public final class NotificationManager {

    /**
     Private constructor to prevent instantiation of utility class.
     */
    private NotificationManager() {
    }

    /**
     Sends a notification to a specific user.
     */
    public static void notifyUser(final String username, final String message) {
        Database db = Database.getInstance();
        User user = db.getUser(username);
        if (user != null) {
            user.addNotification(message);
        }
    }

    /**
     Sends a notification to a list of users.
     */
    public static void notifyUsers(final List<String> usernames, final String message) {
        for (String u : usernames) {
            notifyUser(u, message);
        }
    }

    /**
     Checks for upcoming deadlines and notifies assigned developers if a milestone is due tomorrow.
     */
    public static void checkDeadlines(final Database db, final String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return;
        }
        LocalDate current = LocalDate.parse(timestamp);

        for (Milestone m : db.getMilestones()) {
            if (m.isNotifiedDueTomorrow()) {
                continue;
            }

            if (m.getIsBlocked()) {
                continue;
            }

            LocalDate due = LocalDate.parse(m.getDueDate());
            LocalDate reminderDate = due.minusDays(1);

            if (!current.isBefore(reminderDate) && current.isBefore(due)) {
                m.setNotifiedDueTomorrow(true);
                String msg = "Milestone " + m.getName()
                        + " is due tomorrow. All unresolved tickets are now CRITICAL.";

                notifyUsers(m.getAssignedDevs(), msg);
                updateTicketsToCritical(db, m);
            }
        }
    }

    /**
     Checks if blocked milestones have been unblocked after their due date.
     */
    public static void checkUnblocking(final Database db, final String timestamp) {
        if (timestamp == null || timestamp.isEmpty()) {
            return;
        }
        LocalDate current = LocalDate.parse(timestamp);

        for (Milestone m : db.getMilestones()) {
            if (m.hasDependencies()) {
                boolean isBlocked = m.getIsBlocked();

                if (!isBlocked && !m.isUnblockedNotified()) {
                    m.setUnblockedNotified(true);

                    LocalDate due = LocalDate.parse(m.getDueDate());
                    if (current.isAfter(due)) {
                        String msg = "Milestone " + m.getName()
                                + " was unblocked after due date. "
                                + "All active tickets are now CRITICAL.";
                        notifyUsers(m.getAssignedDevs(), msg);
                        updateTicketsToCritical(db, m);
                    }
                }
            }
        }
    }

    private static void updateTicketsToCritical(final Database db, final Milestone m) {
        for (Integer tId : m.getTickets()) {
            Ticket t = db.getTicket(tId);
            if (t != null && !"CLOSED".equals(t.getStatus())) {
                t.setBusinessPriority("CRITICAL");
            }
        }
    }
}
