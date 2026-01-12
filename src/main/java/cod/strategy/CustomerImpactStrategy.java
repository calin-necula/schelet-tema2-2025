package cod.strategy;
import cod.model.Ticket;
import java.util.List;

public class CustomerImpactStrategy implements MetricStrategy {
    @Override
    public double calculate(List<Ticket> tickets) {
        return 0.0;
    }
}