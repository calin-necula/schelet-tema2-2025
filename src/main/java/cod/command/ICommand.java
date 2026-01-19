package cod.command;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 Interface representing a command in the application.
 */
public interface ICommand {

    /**
     Executes the command logic.
     */
    ObjectNode execute();
}
