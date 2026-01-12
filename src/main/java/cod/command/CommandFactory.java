package cod.command;

import com.fasterxml.jackson.databind.JsonNode;
import cod.command.impl.*;

public class CommandFactory {
    public static ICommand createCommand(String commandName, JsonNode args) {
        switch (commandName) {
            case "startTestingPhase":
                return new StartTestingPhaseCommand(args);
            case "reportTicket":
                return new ReportTicketCommand(args);
            case "viewTickets":
                return new ViewTicketsCommand(args);
            case "createMilestone":
                return new CreateMilestoneCommand(args);
            case "viewMilestones":
                return new ViewMilestonesCommand(args);
            case "assignTicket":
                return new AssignTicketCommand(args);
            case "viewAssignedTickets":
                return new ViewAssignedTicketsCommand(args);
            case "undoAssignTicket":
                return new UndoAssignTicketCommand(args);
            case "addComment":
                return new AddCommentCommand(args);
            case "undoAddComment":
                return new UndoAddCommentCommand(args);
            case "lostInvestors":
                return null;
            default:
                return null;
        }
    }
}