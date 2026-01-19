package cod.strategy;

import cod.model.Ticket;
import java.util.List;

public final class CustomerImpactStrategy implements MetricStrategy {
    @Override
    public double calculate(final List<Ticket> tickets) {
        return 0.0;
    }
}
