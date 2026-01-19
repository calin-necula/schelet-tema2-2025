package cod.strategy;

import cod.model.Ticket;
import java.util.List;

public interface MetricStrategy {

    /**
     Calculates a metric based on the provided list of tickets.
     */
    double calculate(List<Ticket> tickets);
}
