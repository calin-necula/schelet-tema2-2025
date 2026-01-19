package cod.factory;

import cod.model.Bug;
import cod.model.FeatureRequest;
import cod.model.Ticket;
import cod.model.enums.TicketType;

/**
 * Factory class for creating Ticket instances based on their type.
 */
public final class TicketFactory {

    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private TicketFactory() {
    }

    /**
     * Creates a new ticket instance based on the provided type.
     param type the type of the ticket to create
     return a new instance of a Ticket subclass, Bug or FeatureRequest
     throws IllegalArgumentException if the ticket type is unknown
     */
    public static Ticket createTicket(final TicketType type) {
        switch (type) {
            case BUG:
                return new Bug();
            case FEATURE_REQUEST:
                return new FeatureRequest();
            default:
                throw new IllegalArgumentException("Unknown ticket type");
        }
    }
}
