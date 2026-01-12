package cod.factory;

import cod.model.Bug;
import cod.model.FeatureRequest;
import cod.model.Ticket;
import cod.model.enums.TicketType;

public class TicketFactory {
    public static Ticket createTicket(TicketType type) {
        switch (type) {
            case BUG: return new Bug();
            case FEATURE_REQUEST: return new FeatureRequest();
            default: throw new IllegalArgumentException("Unknown ticket type");
        }
    }
}