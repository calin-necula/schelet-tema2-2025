package cod.model;

import cod.model.enums.TicketType;

public class FeatureRequest extends Ticket {
    public FeatureRequest() {
        super();
        this.setType(TicketType.FEATURE_REQUEST);
    }
}
