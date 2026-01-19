package cod.command;

import cod.command.impl.AddCommentCommand;
import cod.command.impl.AppStabilityReportCommand;
import cod.command.impl.AssignTicketCommand;
import cod.command.impl.ChangeStatusTicketCommand;
import cod.command.impl.CreateMilestoneCommand;
import cod.command.impl.GenerateCustomerImpactReportCommand;
import cod.command.impl.GeneratePerformanceReportCommand;
import cod.command.impl.GenerateResolutionEfficiencyReportCommand;
import cod.command.impl.GenerateTicketRiskReportCommand;
import cod.command.impl.ReportTicketCommand;
import cod.command.impl.SearchCommand;
import cod.command.impl.StartTestingPhaseCommand;
import cod.command.impl.UndoAddCommentCommand;
import cod.command.impl.UndoAssignTicketCommand;
import cod.command.impl.UndoChangeStatusTicketCommand;
import cod.command.impl.ViewAssignedTicketsCommand;
import cod.command.impl.ViewMilestonesCommand;
import cod.command.impl.ViewNotificationsCommand;
import cod.command.impl.ViewTicketHistoryCommand;
import com.fasterxml.jackson.databind.JsonNode;

/**
 Factory class for creating command objects based on command names.
 */
public final class CommandFactory {

    /**
     Private constructor to prevent instantiation of utility class.
     */
    private CommandFactory() {
    }

    /**
     Creates a command instance based on the provided command name.
     */
    public static ICommand createCommand(final String commandName, final JsonNode args) {
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
            case "changeStatus":
                return new ChangeStatusTicketCommand(args);
            case "viewTicketHistory":
                return new ViewTicketHistoryCommand(args);
            case "undoChangeStatus":
                return new UndoChangeStatusTicketCommand(args);
            case "search":
                return new SearchCommand(args);
            case "viewNotifications":
                return new ViewNotificationsCommand(args);
            case "generateCustomerImpactReport":
                return new GenerateCustomerImpactReportCommand(args);
            case "generateTicketRiskReport":
                return new GenerateTicketRiskReportCommand(args);
            case "generateResolutionEfficiencyReport":
                return new GenerateResolutionEfficiencyReportCommand(args);
            case "appStabilityReport":
                return new AppStabilityReportCommand(args);
            case "generatePerformanceReport":
                return new GeneratePerformanceReportCommand(args);
            case "lostInvestors":
                return null;
            default:
                return null;
        }
    }
}
