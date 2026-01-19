package cod.command;

import com.fasterxml.jackson.databind.JsonNode;
import cod.command.impl.*;

public class CommandFactory {
    public static ICommand createCommand(String commandName, JsonNode args) {
        switch (commandName) {
            case "startTestingPhase": return new StartTestingPhaseCommand(args);
            case "reportTicket": return new ReportTicketCommand(args);
            case "viewTickets": return new ViewTicketsCommand(args);
            case "createMilestone": return new CreateMilestoneCommand(args);
            case "viewMilestones": return new ViewMilestonesCommand(args);
            case "assignTicket": return new AssignTicketCommand(args);
            case "viewAssignedTickets": return new ViewAssignedTicketsCommand(args);
            case "undoAssignTicket": return new UndoAssignTicketCommand(args);
            case "addComment": return new AddCommentCommand(args);
            case "undoAddComment": return new UndoAddCommentCommand(args);
            case "changeStatus": return new ChangeStatusTicketCommand(args);
            case "viewTicketHistory": return new ViewTicketHistoryCommand(args);
            case "undoChangeStatus": return new UndoChangeStatusTicketCommand(args);
            case "search": return new SearchCommand(args);
            case "viewNotifications": return new ViewNotificationsCommand(args);
            case "generateCustomerImpactReport": return new GenerateCustomerImpactReportCommand(args);
            case "generateTicketRiskReport": return new GenerateTicketRiskReportCommand(args);
            case "generateResolutionEfficiencyReport": return new GenerateResolutionEfficiencyReportCommand(args);
            case "appStabilityReport": return new AppStabilityReportCommand(args);
            case "generatePerformanceReport": return new GeneratePerformanceReportCommand(args);
            case "lostInvestors": return null;
            default: return null;
        }
    }
}