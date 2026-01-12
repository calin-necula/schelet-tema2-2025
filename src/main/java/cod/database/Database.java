package cod.database;

import cod.model.Milestone;
import cod.model.Ticket;
import cod.model.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Database {
    private static Database instance;
    private List<User> users;
    private List<Ticket> tickets;
    private List<Milestone> milestones;
    private boolean isTestingPhase;
    private int ticketIdCounter;
    private LocalDate testingPhaseStartDate;

    private Database() {
        reset();
    }

    public static synchronized Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    public void reset() {
        users = new ArrayList<>();
        tickets = new ArrayList<>();
        milestones = new ArrayList<>();
        isTestingPhase = true;
        ticketIdCounter = 0;
        testingPhaseStartDate = null;
    }

    public void setUsers(List<User> users) { this.users = users; }
    public List<User> getUsers() { return users; }

    public User getUser(String username) {
        return users.stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }

    public void addTicket(Ticket ticket) {
        ticket.setId(ticketIdCounter++);
        tickets.add(ticket);
    }

    public List<Ticket> getTickets() { return tickets; }

    public Ticket getTicket(int id) {
        return tickets.stream()
                .filter(t -> t.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public void addMilestone(Milestone m) {
        milestones.add(m);
    }

    public List<Milestone> getMilestones() {
        return milestones;
    }

    public boolean isTestingPhase() { return isTestingPhase; }
    public void setTestingPhase(boolean testingPhase) { isTestingPhase = testingPhase; }

    public LocalDate getTestingPhaseStartDate() { return testingPhaseStartDate; }
    public void setTestingPhaseStartDate(LocalDate date) { this.testingPhaseStartDate = date; }
}