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

    @JsonIgnore private String initialBusinessPriority;
    @JsonIgnore private String expertiseArea;
    @JsonIgnore private String description;

    private String reportedBy = "";
    private String assignedTo = "";
    private String assignedAt = "";
    private String solvedAt = "";
    private String createdAt = "";

    private List<Comment> comments = new ArrayList<>();

    @JsonIgnore private List<TicketAction> history = new ArrayList<>();

    @JsonIgnore private String severity;
    @JsonIgnore private String expectedBehavior;
    @JsonIgnore private String actualBehavior;
    @JsonIgnore private String frequency;
    @JsonIgnore private String environment;
    @JsonIgnore private Integer errorCode;
    @JsonIgnore private String businessValue;
    @JsonIgnore private String customerDemand;
    @JsonIgnore private String uiElementId;
    @JsonIgnore private Integer usabilityScore;
    @JsonIgnore private String suggestedFix;

    public Ticket() {
        this.status = "OPEN";
    }

    public void addHistory(TicketAction action) {
        this.history.add(action);
    }

    public List<TicketAction> getHistory() {
        return history;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public TicketType getType() { return type; }
    public void setType(TicketType type) { this.type = type; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBusinessPriority() { return businessPriority; }
    public void setBusinessPriority(String businessPriority) {
        this.businessPriority = businessPriority;
        if (this.initialBusinessPriority == null) {
            this.initialBusinessPriority = businessPriority;
        }
    }

    public void resetInitialPriority(String p) {
        this.initialBusinessPriority = p;
        this.businessPriority = p;
    }

    public String getInitialBusinessPriority() { return initialBusinessPriority; }
    public void setComputedPriority(String priority) {
        this.businessPriority = priority;
    }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getExpertiseArea() { return expertiseArea; }
    public void setExpertiseArea(String expertiseArea) { this.expertiseArea = expertiseArea; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReportedBy() { return reportedBy; }
    public void setReportedBy(String reportedBy) { this.reportedBy = reportedBy; }
    public String getAssignedTo() { return assignedTo; }
    public void setAssignedTo(String assignedTo) { this.assignedTo = assignedTo; }
    public String getAssignedAt() { return assignedAt; }
    public void setAssignedAt(String assignedAt) { this.assignedAt = assignedAt; }
    public String getSolvedAt() { return solvedAt; }
    public void setSolvedAt(String solvedAt) { this.solvedAt = solvedAt; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public List<Comment> getComments() { return comments; }
    public void setComments(List<Comment> comments) { this.comments = comments; }
    public void addComment(Comment comment) {
        this.comments.add(comment);
    }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getExpectedBehavior() { return expectedBehavior; }
    public void setExpectedBehavior(String expectedBehavior) { this.expectedBehavior = expectedBehavior; }
    public String getActualBehavior() { return actualBehavior; }
    public void setActualBehavior(String actualBehavior) { this.actualBehavior = actualBehavior; }
    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    public Integer getErrorCode() { return errorCode; }
    public void setErrorCode(Integer errorCode) { this.errorCode = errorCode; }
    public String getBusinessValue() { return businessValue; }
    public void setBusinessValue(String businessValue) { this.businessValue = businessValue; }
    public String getCustomerDemand() { return customerDemand; }
    public void setCustomerDemand(String customerDemand) { this.customerDemand = customerDemand; }
    public String getUiElementId() { return uiElementId; }
    public void setUiElementId(String uiElementId) { this.uiElementId = uiElementId; }
    public Integer getUsabilityScore() { return usabilityScore; }
    public void setUsabilityScore(Integer usabilityScore) { this.usabilityScore = usabilityScore; }
    public String getSuggestedFix() { return suggestedFix; }
    public void setSuggestedFix(String suggestedFix) { this.suggestedFix = suggestedFix; }
}