package cod.command;
import com.fasterxml.jackson.databind.node.ObjectNode;

public interface ICommand {
    ObjectNode execute();
}