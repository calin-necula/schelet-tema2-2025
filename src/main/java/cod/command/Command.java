package cod.command;

/**
 Interface representing a generic command in the command pattern.
 */
public interface Command {

    /**
     Executes the specific logic of the command.
     */
    void execute();

    /**
     Undoes the logic executed by the command, reverting changes if applicable.
     */
    void undo();
}
