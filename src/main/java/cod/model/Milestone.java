package cod.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import cod.database.Database;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@JsonPropertyOrder({
        "name", "blockingFor", "dueDate", "createdAt", "tickets", "assignedDevs",
        "createdBy", "status", "isBlocked", "daysUntilDue", "overdueBy",
        "openTickets", "closedTickets", "completionPercentage", "repartition"
})
public final class Milestone {
    private static final double SCALE_100 = 100.0;

    private String name;
    private List<String> blockingFor = new ArrayList<>();
    private String dueDate;
    private String createdAt;
    private List<Integer> tickets = new ArrayList<>();
    private List<String> assignedDevs = new ArrayList<>();
    private String createdBy;
    private String status = "ACTIVE";

    @JsonIgnore
    private int daysUntilDueVal = 0;
    @JsonIgnore
    private int overdueByVal = 0;
    @JsonIgnore
    private boolean notifiedDueTomorrow = false;
    @JsonIgnore
    private boolean unblockedNotified = false;

    public Milestone() {
    }

    /**
     Gets the name of the milestone.
     */
    public String getName() {
        return name;
    }

    /**
     Sets the name of the milestone.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     Gets the list of milestones blocked by this one.
     */
    public List<String> getBlockingFor() {
        return blockingFor;
    }

    /**
     Sets the list of milestones blocked by this one.
     */
    public void setBlockingFor(final List<String> blockingFor) {
        this.blockingFor = blockingFor;
    }

    /**
     Gets the due date.
     */
    public String getDueDate() {
        return dueDate;
    }

    /**
     Sets the due date.
     */
    public void setDueDate(final String dueDate) {
        this.dueDate = dueDate;
    }

    /**
     Gets the creation date.
     */
    public String getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets the creation date.
     */
    public void setCreatedAt(final String createdAt) {
        this.createdAt = createdAt;
    }

    /**
     Gets the list of ticket IDs associated with this milestone.
     */
    public List<Integer> getTickets() {
        return tickets;
    }

    /**
     Sets the list of ticket IDs.
     */
    public void setTickets(final List<Integer> tickets) {
        this.tickets = tickets;
    }

    /**
     Gets the list of assigned developers.
     */
    public List<String> getAssignedDevs() {
        return assignedDevs;
    }

    /**
     Sets the list of assigned developers.
     */
    public void setAssignedDevs(final List<String> assignedDevs) {
        this.assignedDevs = assignedDevs;
    }

    /**
     Gets the creator's username.
     */
    public String getCreatedBy() {
        return createdBy;
    }

    /**
     Sets the creator's username.
     */
    public void setCreatedBy(final String createdBy) {
        this.createdBy = createdBy;
    }

    /**
     Gets the status of the milestone.
     */
    public String getStatus() {
        return status;
    }

    /**
     Sets the status of the milestone.
     */
    public void setStatus(final String status) {
        this.status = status;
    }

    /**
     Checks if the due tomorrow notification was sent.
     */
    public boolean isNotifiedDueTomorrow() {
        return notifiedDueTomorrow;
    }

    /**
     Sets the due tomorrow notification flag.
     */
    public void setNotifiedDueTomorrow(final boolean notifiedDueTomorrow) {
        this.notifiedDueTomorrow = notifiedDueTomorrow;
    }

    /**
     Checks if the unblocked notification was sent.
     */
    public boolean isUnblockedNotified() {
        return unblockedNotified;
    }

    /**
     Sets the unblocked notification flag.
     */
    public void setUnblockedNotified(final boolean unblockedNotified) {
        this.unblockedNotified = unblockedNotified;
    }

    /**
     Checks if this milestone is blocked by other milestones.
     */
    public boolean getIsBlocked() {
        Database db = Database.getInstance();
        for (Milestone other : db.getMilestones()) {
            if (other.getBlockingFor().contains(this.name)) {
                boolean isBlockingActive = other.getTickets().stream()
                        .map(db::getTicket)
                        .anyMatch(t -> t != null && !"CLOSED".equals(t.getStatus()));

                if (isBlockingActive) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     Checks if this milestone depends on other milestones.
     */
    @JsonIgnore
    public boolean hasDependencies() {
        Database db = Database.getInstance();
        for (Milestone other : db.getMilestones()) {
            if (other.getBlockingFor().contains(this.name)) {
                return true;
            }
        }
        return false;
    }

    /**
     Gets the number of days until the milestone is due.
     */
    public Integer getDaysUntilDue() {
        return this.daysUntilDueVal;
    }

    /**
     Gets the number of days the milestone is overdue.
     */
    public Integer getOverdueBy() {
        return this.overdueByVal;
    }

    /**
     Gets the list of open ticket IDs.
     */
    public List<Integer> getOpenTickets() {
        Database db = Database.getInstance();
        return tickets.stream()
                .filter(id -> {
                    Ticket t = db.getTicket(id);
                    return t != null && !"CLOSED".equals(t.getStatus());
                })
                .collect(Collectors.toList());
    }

    /**
     Gets the list of closed ticket IDs.
     */
    public List<Integer> getClosedTickets() {
        Database db = Database.getInstance();
        return tickets.stream()
                .filter(id -> {
                    Ticket t = db.getTicket(id);
                    return t != null && "CLOSED".equals(t.getStatus());
                })
                .collect(Collectors.toList());
    }

    /**
     Calculates the completion percentage of the milestone.
     */
    public Double getCompletionPercentage() {
        if (tickets.isEmpty()) {
            return 0.0;
        }
        double closed = getClosedTickets().size();
        return Math.round((closed / tickets.size()) * SCALE_100) / SCALE_100;
    }

    /**
     Gets the repartition of tickets among developers.
     */
    public List<RepartitionEntry> getRepartition() {
        Database db = Database.getInstance();
        List<RepartitionEntry> list = new ArrayList<>();
        for (String devUser : assignedDevs) {
            List<Integer> devTickets = tickets.stream()
                    .filter(id -> {
                        Ticket t = db.getTicket(id);
                        return t != null && devUser.equals(t.getAssignedTo());
                    })
                    .collect(Collectors.toList());
            list.add(new RepartitionEntry(devUser, devTickets));
        }
        return list;
    }

    public static final class RepartitionEntry {
        private String developer;
        private List<Integer> assignedTickets;

        public RepartitionEntry(final String dev, final List<Integer> t) {
            this.developer = dev;
            this.assignedTickets = t;
        }

        public String getDeveloper() {
            return developer;
        }

        public List<Integer> getAssignedTickets() {
            return assignedTickets;
        }
    }

    /**
     Calculates time-related fields (daysUntilDue, overdueBy) based on current timestamp.
     */
    public void calculateTimeFields(final String currentTimestamp) {
        if (currentTimestamp == null || currentTimestamp.isEmpty()) {
            return;
        }

        LocalDate comparisonDate = LocalDate.parse(currentTimestamp);

        if ("COMPLETED".equals(this.status)) {
            Database db = Database.getInstance();
            LocalDate maxSolved = null;
            for (Integer tId : tickets) {
                Ticket t = db.getTicket(tId);
                if (t != null && t.getSolvedAt() != null && !t.getSolvedAt().isEmpty()) {
                    LocalDate solved = LocalDate.parse(t.getSolvedAt());
                    if (maxSolved == null || solved.isAfter(maxSolved)) {
                        maxSolved = solved;
                    }
                }
            }
            if (maxSolved != null) {
                comparisonDate = maxSolved;
            }
        }

        LocalDate due = LocalDate.parse(this.dueDate);
        long diff = ChronoUnit.DAYS.between(comparisonDate, due);

        if (diff >= 0) {
            this.daysUntilDueVal = (int) diff + 1;
            this.overdueByVal = 0;
        } else {
            this.daysUntilDueVal = 0;
            this.overdueByVal = (int) Math.abs(diff) + 1;
        }
    }
}
