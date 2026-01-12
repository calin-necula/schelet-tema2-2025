package cod.model;

import cod.model.enums.TicketType;

public class Bug extends Ticket {
    public Bug() {
        super();
        this.setType(TicketType.BUG);
    }
}