package cod.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.ArrayList;
import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "role", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = Reporter.class, name = "REPORTER"),
        @JsonSubTypes.Type(value = Developer.class, name = "DEVELOPER"),
        @JsonSubTypes.Type(value = Manager.class, name = "MANAGER")
})
public abstract class User {
    protected String username;
    protected String email;
    protected String role;

    @JsonIgnore
    private List<String> notifications = new ArrayList<>();

    /**
     * Gets the username.
     */
    public final String getUsername() {
        return username;
    }

    /**
     * Sets the username.
     */
    public final void setUsername(final String username) {
        this.username = username;
    }

    /**
     * Gets the email.
     */
    public final String getEmail() {
        return email;
    }

    /**
     * Sets the email.
     */
    public final void setEmail(final String email) {
        this.email = email;
    }

    /**
     * Gets the user role.
     */
    public final String getRole() {
        return role;
    }

    /**
     * Sets the user role.
     */
    public final void setRole(final String role) {
        this.role = role;
    }

    /**
     * Gets the list of notifications.
     */
    public final List<String> getNotifications() {
        return notifications;
    }

    /**
     * Sets the list of notifications.
     */
    public final void setNotifications(final List<String> notifications) {
        this.notifications = notifications;
    }

    /**
     * Adds a single notification to the user's list.
     */
    public final void addNotification(final String notification) {
        this.notifications.add(notification);
    }

    /**
     * Clears all notifications for the user.
     */
    public final void clearNotifications() {
        this.notifications.clear();
    }
}
