package cod.command.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import cod.command.ICommand;
import cod.database.Database;
import cod.model.Ticket;
import cod.model.TicketAction;

import java.util.List;
import java.util.stream.Collectors;

public final class ViewTicketHistoryCommand implements ICommand {
    private final JsonNode args;
    private final ObjectMapper mapper = new ObjectMapper();

    public ViewTicketHistoryCommand(final JsonNode args) {
        this.args = args;
    }

    @Override
    public ObjectNode execute() {
        Database db = Database.getInstance();
        ObjectNode result = mapper.createObjectNode();
        String username = args.has("username") ? args.get("username").asText() : "";
        String timestamp = args.has("timestamp") ? args.get("timestamp").asText() : "";

        result.put("command", "viewTicketHistory");
        result.put("username", username);
        result.put("timestamp", timestamp);

        ArrayNode historyArray = result.putArray("ticketHistory");

        List<Ticket> relevantTickets = db.getTickets().stream()
                .filter(t -> {
                    if (username.equals(t.getAssignedTo())) {
                        return true;
                    }
                    for (TicketAction action : t.getHistory()) {
                        if (username.equals(action.getBy())) {
                            return true;
                        }
                    }
                    return false;
                })
                .collect(Collectors.toList());

        for (Ticket t : relevantTickets) {
            ObjectNode ticketNode = mapper.createObjectNode();
            ticketNode.put("id", t.getId());
            ticketNode.put("title", t.getTitle());
            ticketNode.put("status", t.getStatus());

            ArrayNode actionsNode = ticketNode.putArray("actions");
            for (TicketAction action : t.getHistory()) {
                actionsNode.add(mapper.valueToTree(action));
            }

            ticketNode.set("comments", mapper.valueToTree(t.getComments()));
            historyArray.add(ticketNode);
        }

        return result;
    }
}
