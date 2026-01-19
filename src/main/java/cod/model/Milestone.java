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
public class Milestone {
    private String name;
    private List<String> blockingFor = new ArrayList<>();
    private String dueDate;
    private String createdAt;
    private List<Integer> tickets = new ArrayList<>();
    private List<String> assignedDevs = new ArrayList<>();
    private String createdBy;
    private String status = "ACTIVE";

    @JsonIgnore public int daysUntilDueVal = 0;
    @JsonIgnore public int overdueByVal = 0;

    // --- Flag-uri Notificări ---
    @JsonIgnore private boolean notifiedDueTomorrow = false;
    @JsonIgnore private boolean unblockedNotified = false;

    public Milestone() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<String> getBlockingFor() { return blockingFor; }
    public void setBlockingFor(List<String> blockingFor) { this.blockingFor = blockingFor; }
    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public List<Integer> getTickets() { return tickets; }
    public void setTickets(List<Integer> tickets) { this.tickets = tickets; }
    public List<String> getAssignedDevs() { return assignedDevs; }
    public void setAssignedDevs(List<String> assignedDevs) { this.assignedDevs = assignedDevs; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isNotifiedDueTomorrow() { return notifiedDueTomorrow; }
    public void setNotifiedDueTomorrow(boolean notifiedDueTomorrow) { this.notifiedDueTomorrow = notifiedDueTomorrow; }
    public boolean isUnblockedNotified() { return unblockedNotified; }
    public void setUnblockedNotified(boolean unblockedNotified) { this.unblockedNotified = unblockedNotified; }

    public boolean getIsBlocked() {
        Database db = Database.getInstance();
        for (Milestone other : db.getMilestones()) {
            if (other.getBlockingFor().contains(this.name)) {
                boolean isBlockingActive = other.getTickets().stream()
                        .map(id -> db.getTicket(id))
                        .anyMatch(t -> t != null && !"CLOSED".equals(t.getStatus()));

                if (isBlockingActive) return true;
            }
        }
        return false;
    }

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

    public Integer getDaysUntilDue() { return this.daysUntilDueVal; }
    public Integer getOverdueBy() { return this.overdueByVal; }

    public List<Integer> getOpenTickets() {
        Database db = Database.getInstance();
        return tickets.stream()
                .filter(id -> {
                    Ticket t = db.getTicket(id);
                    return t != null && !"CLOSED".equals(t.getStatus());
                })
                .collect(Collectors.toList());
    }

    public List<Integer> getClosedTickets() {
        Database db = Database.getInstance();
        return tickets.stream()
                .filter(id -> {
                    Ticket t = db.getTicket(id);
                    return t != null && "CLOSED".equals(t.getStatus());
                })
                .collect(Collectors.toList());
    }

    public Double getCompletionPercentage() {
        if (tickets.isEmpty()) return 0.0;
        double closed = getClosedTickets().size();
        return Math.round((closed / tickets.size()) * 100.0) / 100.0;
    }

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

    public static class RepartitionEntry {
        public String developer;
        public List<Integer> assignedTickets;
        public RepartitionEntry(String dev, List<Integer> t) { this.developer = dev; this.assignedTickets = t; }
    }

    public void calculateTimeFields(String currentTimestamp) {
        if (currentTimestamp == null || currentTimestamp.isEmpty()) return;

        LocalDate comparisonDate = LocalDate.parse(currentTimestamp);

        if ("COMPLETED".equals(this.status)) {
            Database db = Database.getInstance();
            LocalDate maxSolved = null;
            for(Integer tId : tickets) {
                Ticket t = db.getTicket(tId);
                if(t != null && t.getSolvedAt() != null && !t.getSolvedAt().isEmpty()) {
                    LocalDate solved = LocalDate.parse(t.getSolvedAt());
                    if(maxSolved == null || solved.isAfter(maxSolved)) {
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