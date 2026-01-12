package cod.strategy;
import cod.model.Ticket;
import java.util.List;

public interface MetricStrategy {
    double calculate(List<Ticket> tickets);
}