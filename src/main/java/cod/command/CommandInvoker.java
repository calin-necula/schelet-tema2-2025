package cod.command;

import java.util.Stack;

/**
 Invoker class that handles the execution and undoing of commands.
 It maintains a history of executed commands to support undo functionality.
 */
public final class CommandInvoker {
    private final Stack<Command> history = new Stack<>();

    /**
     Executes the given command and adds it to the history stack.
     */
    public void executeCommand(final Command command) {
        command.execute();
        history.push(command);
    }

    /**
     Undoes the last command executed, removing it from the history stack.
     If the history is empty, this method does nothing.
     */
    public void undoLastCommand() {
        if (!history.isEmpty()) {
            final Command cmd = history.pop();
            cmd.undo();
        }
    }
}
