package cod.database;

import cod.model.Milestone;
import cod.model.Ticket;
import cod.model.User;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 Singleton Database class to store application data.
 */
public final class Database {
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

    /**
     returns the singleton instance of the Database.
     return the singleton instance
     */
    public static synchronized Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    /**
     Resets the database to its initial state.
     */
    public void reset() {
        users = new ArrayList<>();
        tickets = new ArrayList<>();
        milestones = new ArrayList<>();
        isTestingPhase = true;
        ticketIdCounter = 0;
        testingPhaseStartDate = null;
    }

    /**
     Sets the list of users.
     param users the list of users to set
     */
    public void setUsers(final List<User> users) {
        this.users = users;
    }

    public List<User> getUsers() {
        return users;
    }

    /**
     Retrieves a user by their username.
     return the User object if found, otherwise null
     */
    public User getUser(final String username) {
        return users.stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }

    /**
     Adds a ticket to the database and assigns it an ID.
     */
    public void addTicket(final Ticket ticket) {
        ticket.setId(ticketIdCounter++);
        tickets.add(ticket);
    }

    public List<Ticket> getTickets() {
        return tickets;
    }

    /**
     Retrieves a ticket by its ID.
     param id the ID of the ticket
     return the Ticket object if found, otherwise null
     */
    public Ticket getTicket(final int id) {
        return tickets.stream()
                .filter(t -> t.getId() == id)
                .findFirst()
                .orElse(null);
    }

    /**
     Adds a milestone to the database.
     */
    public void addMilestone(final Milestone m) {
        milestones.add(m);
    }

    public List<Milestone> getMilestones() {
        return milestones;
    }

    public boolean isTestingPhase() {
        return isTestingPhase;
    }

    public void setTestingPhase(final boolean testingPhase) {
        isTestingPhase = testingPhase;
    }

    public LocalDate getTestingPhaseStartDate() {
        return testingPhaseStartDate;
    }

    public void setTestingPhaseStartDate(final LocalDate date) {
        this.testingPhaseStartDate = date;
    }
}
