package source;

import java.util.Stack;

/**
 * Handles logging the indented debug messages for each node visited
 * by the visitor classes.
 */
public class VisitorDebugger {
    /**
     * Name of the action currently being performed.
     * i.e. static checking, compiling, etc
     */
    private final String action;
    /**
     * Error handler to write debug messages to
     */
    private final Errors errors;
    /**
     * Track the node currently being executed
     */
    private final Stack<String> nodeStack = new Stack<>();

    /**
     * Contract a new debugging stack
     *
     * @param action Name of the current action being performed
     * @param errors Error handler to print debug message to
     */
    public VisitorDebugger(String action, Errors errors) {
        this.action = action;
        this.errors = errors;
    }

    /**
     * Visit a new node and log a debug message
     */
    public void beginDebug(String node) {
        nodeStack.push(node);
        errors.debugMessage("Begin " + action + " of " + node);
        errors.incDebug();
    }

    /**
     * Exit the current node and log a debug message
     */
    public void endDebug(String node) {
        errors.decDebug();
        errors.debugMessage("End " + action + " of " + node);

        if (nodeStack.isEmpty()) {
            /* This indicates an error in the code interpreter - always prints */
            errors.debugPrint("*** End of node " + node
                    + " has no matching start");
        } else {
            String popped = nodeStack.pop();
            if (!(node.equals(popped))) {
                /* This indicates an error in the code interpreter - always prints */
                errors.debugPrint("*** End node " + node
                        + " does not match start node " + popped);
            }
        }
    }
}
