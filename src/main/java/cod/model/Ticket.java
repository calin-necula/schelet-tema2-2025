package cod.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import cod.model.enums.TicketType;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
        "id", "type", "title", "businessPriority", "status",
        "createdAt", "solvedAt", "reportedBy", "matchingWords"
})
public class Ticket {
    private int id;
    private TicketType type;
    private String title;
    private String businessPriority;
    private String status;

    @JsonIgnore
    private String initialBusinessPriority;
    @JsonIgnore
    private String expertiseArea;
    @JsonIgnore
    private String description;

    private String reportedBy = "";
    private String assignedTo = "";
    private String assignedAt = "";
    private String solvedAt = "";
    private String createdAt = "";

    private List<Comment> comments = new ArrayList<>();

    @JsonIgnore
    private List<TicketAction> history = new ArrayList<>();

    @JsonIgnore
    private String severity;
    @JsonIgnore
    private String expectedBehavior;
    @JsonIgnore
    private String actualBehavior;
    @JsonIgnore
    private String frequency;
    @JsonIgnore
    private String environment;
    @JsonIgnore
    private Integer errorCode;
    @JsonIgnore
    private String businessValue;
    @JsonIgnore
    private String customerDemand;
    @JsonIgnore
    private String uiElementId;
    @JsonIgnore
    private Integer usabilityScore;
    @JsonIgnore
    private String suggestedFix;

    public Ticket() {
        this.status = "OPEN";
    }

    /**
     Adds an action to the ticket's history.
     */
    public void addHistory(final TicketAction action) {
        this.history.add(action);
    }

    /**
     Gets the history of actions on this ticket.
     */
    public List<TicketAction> getHistory() {
        return history;
    }

    /**
     Gets the ticket ID.
     */
    public int getId() {
        return id;
    }

    /**
     Sets the ticket ID.
     */
    public void setId(final int id) {
        this.id = id;
    }

    /**
     Gets the ticket type.
     */
    public TicketType getType() {
        return type;
    }

    /**
     Sets the ticket type.
     */
    public void setType(final TicketType type) {
        this.type = type;
    }

    /**
     Gets the ticket title.
     */
    public String getTitle() {
        return title;
    }

    /**
     Sets the ticket title.
     */
    public void setTitle(final String title) {
        this.title = title;
    }

    /**
     Gets the business priority.
     */
    public String getBusinessPriority() {
        return businessPriority;
    }

    /**
     Sets the business priority.
     */
    public void setBusinessPriority(final String businessPriority) {
        this.businessPriority = businessPriority;
        if (this.initialBusinessPriority == null) {
            this.initialBusinessPriority = businessPriority;
        }
    }

    /**
     Resets the initial priority and current priority to the specified value.
     */
    public void resetInitialPriority(final String p) {
        this.initialBusinessPriority = p;
        this.businessPriority = p;
    }

    /**
     Gets the initial business priority.
     */
    public String getInitialBusinessPriority() {
        return initialBusinessPriority;
    }

    /**
     Sets the computed priority.
     */
    public void setComputedPriority(final String priority) {
        this.businessPriority = priority;
    }

    /**
     Gets the status.
     */
    public String getStatus() {
        return status;
    }

    /**
     Sets the status.
     */
    public void setStatus(final String status) {
        this.status = status;
    }

    /**
     Gets the expertise area.
     */
    public String getExpertiseArea() {
        return expertiseArea;
    }

    /**
     Sets the expertise area.
     */
    public void setExpertiseArea(final String expertiseArea) {
        this.expertiseArea = expertiseArea;
    }

    /**
     Gets the description.
     */
    public String getDescription() {
        return description;
    }

    /**
     Sets the description.
     */
    public void setDescription(final String description) {
        this.description = description;
    }

    /**
     Gets the reporter username.
     */
    public String getReportedBy() {
        return reportedBy;
    }

    /**
     Sets the reporter username.
     */
    public void setReportedBy(final String reportedBy) {
        this.reportedBy = reportedBy;
    }

    /**
     Gets the assignee username.
     */
    public String getAssignedTo() {
        return assignedTo;
    }

    /**
     Sets the assignee username.
     */
    public void setAssignedTo(final String assignedTo) {
        this.assignedTo = assignedTo;
    }

    /**
     Gets the assignment timestamp.
     */
    public String getAssignedAt() {
        return assignedAt;
    }

    /**
     Sets the assignment timestamp.
     */
    public void setAssignedAt(final String assignedAt) {
        this.assignedAt = assignedAt;
    }

    /**
     Gets the solved timestamp.
     */
    public String getSolvedAt() {
        return solvedAt;
    }

    /**
     Sets the solved timestamp.
     */
    public void setSolvedAt(final String solvedAt) {
        this.solvedAt = solvedAt;
    }

    /**
     Gets the creation timestamp.
     */
    public String getCreatedAt() {
        return createdAt;
    }

    /**
     Sets the creation timestamp.
     */
    public void setCreatedAt(final String createdAt) {
        this.createdAt = createdAt;
    }

    /**
     Gets the list of comments.
     */
    public List<Comment> getComments() {
        return comments;
    }

    /**
     Sets the list of comments.
     */
    public void setComments(final List<Comment> comments) {
        this.comments = comments;
    }

    /**
     Adds a comment to the ticket.
     */
    public void addComment(final Comment comment) {
        this.comments.add(comment);
    }

    /**
     Gets the severity.
     */
    public String getSeverity() {
        return severity;
    }

    /**
     Sets the severity.
     */
    public void setSeverity(final String severity) {
        this.severity = severity;
    }

    /**
     Gets the expected behavior.
     */
    public String getExpectedBehavior() {
        return expectedBehavior;
    }

    /**
     Sets the expected behavior.
     */
    public void setExpectedBehavior(final String expectedBehavior) {
        this.expectedBehavior = expectedBehavior;
    }

    /**
     Gets the actual behavior.
     */
    public String getActualBehavior() {
        return actualBehavior;
    }

    /**
     Sets the actual behavior.
     */
    public void setActualBehavior(final String actualBehavior) {
        this.actualBehavior = actualBehavior;
    }

    /**
     Gets the frequency.
     */
    public String getFrequency() {
        return frequency;
    }

    /**
     Sets the frequency.
     */
    public void setFrequency(final String frequency) {
        this.frequency = frequency;
    }

    /**
     Gets the environment.
     */
    public String getEnvironment() {
        return environment;
    }

    /**
     Sets the environment.
     */
    public void setEnvironment(final String environment) {
        this.environment = environment;
    }

    /**
     Gets the error code.
     */
    public Integer getErrorCode() {
        return errorCode;
    }

    /**
     Sets the error code.
     */
    public void setErrorCode(final Integer errorCode) {
        this.errorCode = errorCode;
    }

    /**
     Gets the business value.
     */
    public String getBusinessValue() {
        return businessValue;
    }

    /**
     Sets the business value.
     */
    public void setBusinessValue(final String businessValue) {
        this.businessValue = businessValue;
    }

    /**
     Gets the customer demand.
     */
    public String getCustomerDemand() {
        return customerDemand;
    }

    /**
     Sets the customer demand.

     */
    public void setCustomerDemand(final String customerDemand) {
        this.customerDemand = customerDemand;
    }

    /**
     Gets the UI element ID.
     */
    public String getUiElementId() {
        return uiElementId;
    }

    /**
     Sets the UI element ID.
     */
    public void setUiElementId(final String uiElementId) {
        this.uiElementId = uiElementId;
    }

    /**
     Gets the usability score.
     */
    public Integer getUsabilityScore() {
        return usabilityScore;
    }

    /**
     Sets the usability score.
     */
    public void setUsabilityScore(final Integer usabilityScore) {
        this.usabilityScore = usabilityScore;
    }

    /**
     Gets the suggested fix.
     */
    public String getSuggestedFix() {
        return suggestedFix;
    }

    /**
     Sets the suggested fix.
     */
    public void setSuggestedFix(final String suggestedFix) {
        this.suggestedFix = suggestedFix;
    }
}
