package cod.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "milestone", "from", "to", "by", "timestamp", "action" })
public class TicketAction {
    private String milestone;
    private String from;
    private String to;
    private String by;
    private String timestamp;
    private String action;

    public TicketAction() {}

    public String getMilestone() { return milestone; }
    public void setMilestone(String milestone) { this.milestone = milestone; }

    public String getFrom() { return from; }
    public void setFrom(String from) { this.from = from; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public String getBy() { return by; }
    public void setBy(String by) { this.by = by; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}