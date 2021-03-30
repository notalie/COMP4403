package interpreter;

import java.util.*;

import java_cup.runtime.ComplexSymbolFactory.Location;
import source.VisitorDebugger;
import source.Errors;
import syms.SymEntry;
import syms.Type;
import tree.*;
import interpreter.Value.*;

import java.io.BufferedReader;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Execute the abstract syntax tree directly
 */
public class Interpreter implements StatementVisitor, ExpTransform<Value> {

    /**
     * Buffered input of stdin
     */
    private final BufferedReader in;

    /**
     * Errors are reported through the error handler.
     */
    private final Errors errors;

    /**
     * Debug messages are reported through the visitor debugger.
     */
    private final VisitorDebugger debug;

    /**
     * Program output stream
     */
    private final PrintStream outStream;

    /**
     * Runtime stack containing values of variables for each
     * procedure's stack frame
     **/
    private Frame currentFrame;

    /**
     * Construct a new interpreter
     *
     * @param errors      Error message handler
     * @param inputStream Program input stream
     * @param outStream   Program output stream
     */
    public Interpreter(Errors errors, InputStream inputStream,
                       PrintStream outStream) {
        this.errors = errors;
        this.debug = new VisitorDebugger("executing", errors);
        this.in = new BufferedReader(new InputStreamReader(inputStream));
        this.outStream = outStream;
    }

    /**
     * Execute the main procedure
     *
     * @param node Abstract syntax tree for the main program.
     */
    public void executeCode(DeclNode.ProcedureNode node) {
        beginExec("Program");
        SymEntry.ProcedureEntry procEntry = node.getProcEntry();
        /* Setup the main frame */
        currentFrame = new Frame(null, null, procEntry);
        /* Execute main procedure code body */
        visitBlockNode(procEntry.getBlock());
        endExec("Program");
    }

    /* Statement Execution */

    /**
     * Execute code for a block statement
     */
    public void visitBlockNode(StatementNode.BlockNode node) {
        beginExec("Block");
        /* Execute the body of the block */
        node.getBody().accept(this);
        endExec("Block");
    }

    /**
     * Execute code for an error statement
     */
    public void visitStatementErrorNode(StatementNode.ErrorNode node) {
        errors.fatal("PL0 Internal error: interpreting Statement Error Node",
                node.getLocation());
    }

    /**
     * Execute code for a statement list - executes each statement sequentially
     */
    public void visitStatementListNode(StatementNode.ListNode node) {
        beginExec("StatementList");
        for (StatementNode statement : node.getStatements()) {
            statement.accept(this);
        }
        endExec("StatementList");
    }

    /**
     * Lookup the closest frame with the same static level as the variable
     * and assign the value to the variables offset within that frame.
     *
     * @param lValue  The address of the variable to assign the value to.
     * @param value The value to assign.
     */
    private void assignValue(Value lValue, Value value) {
        /* Resolve the frame containing the variable node */
        Frame frame = currentFrame.lookupFrame(lValue.getAddressLevel());
        /* Assign the variables value to the offset in the frame */
        frame.assign(lValue.getAddressOffset(), value);
    }



    /**
     * Execute code for an assignment statement
     */
    public void visitAssignmentNode(StatementNode.AssignmentNode node) {
        beginExec("Assignment");
        /* Evaluate the code to be assigned */
        Value value = node.getExp().evaluate(this);
        /* Assign the value to the variables offset */
        Value lValue = node.getVariable().evaluate(this);
        assignValue(lValue, value);
        endExec("Assignment");
    }

    /**
     * Execute code for a read statement - read an int from standard input
     */
    public void visitReadNode(StatementNode.ReadNode node) {
        beginExec("Read");
        /* Read next int from standard input */
        IntegerValue result = null;
        try {
            result = new IntegerValue(Integer.parseInt(in.readLine()));
        } catch (Exception e) {
            runtime("invalid value read - must be an integer",
                    node.getLocation(), currentFrame);
            // Never reached
        }
        Value lValue = node.getLValue().evaluate(this);
        assignValue(lValue,result);
        endExec("Read");
    }

    /**
     * Execute code for a write statement
     */
    public void visitWriteNode(StatementNode.WriteNode node) {
        beginExec("Write");
        /* Evaluate the write expression */
        int result = node.getExp().evaluate(this).getInteger();
        /* Print the result to the outStream */
        outStream.println(result);
        endExec("Write");
    }

    /**
     * Execute code for a call statement
     */
    public void visitCallNode(StatementNode.CallNode node) {
        beginExec("Call");
        /* Decent to the executing procedures frame */
        currentFrame = currentFrame.enterFrame(node.getEntry());
        /* Resolve the code block to call and execute the block */
        node.getEntry().getBlock().accept(this);
        /* Return to the parent frame */
        currentFrame = currentFrame.exitFrame();
        endExec("Call");
    }


    /**
     * Execute code for an if statement
     */
    public void visitIfNode(StatementNode.IfNode node) {
        beginExec("If");
        ExpNode condition = node.getCondition();
        if (condition.evaluate(this).getInteger() == Type.TRUE_VALUE) {
            /* Execute then statement if condition evaluates to true */
            node.getThenStmt().accept(this);
        } else {
            /* Execute else statement if condition evaluates to false */
            node.getElseStmt().accept(this);
        }
        endExec("If");
    }

    /**
     * Execute code for a while statement
     */
    public void visitWhileNode(StatementNode.WhileNode node) {
        beginExec("While");
        /* Execute loop statement while the condition is true */
        ExpNode condition = node.getCondition();
        while (condition.evaluate(this).getInteger() == Type.TRUE_VALUE) {
            node.getLoopStmt().accept(this);
        }
        endExec("While");
    }


    /* Expression Evaluations */

    /**
     * Expression evaluation for an error node - should never be reached
     */
    public ErrorValue visitErrorExpNode(ExpNode.ErrorNode node) {
        /* Error when error node is evaluated */
        errors.fatal("PL0 Internal error: attempt to evaluate ErrorExpNode",
                node.getLocation());
        return null; // Never reached
    }

    /**
     * Expression evaluation for a constant - resolve to the constant's value
     */
    public IntegerValue visitConstNode(ExpNode.ConstNode node) {
        beginExec("ConstNode");
        IntegerValue result = new IntegerValue(node.getValue());
        endExec("ConstNode");
        return result;
    }

    /**
     * Expression evaluation for an identifier node - should never be reached
     */
    public Value visitIdentifierNode(ExpNode.IdentifierNode node) {
        /* Error when identifier node is evaluated, identifier nodes should
         * be eliminated by the semantic syntax process
         */
        errors.fatal("PL0 Internal error: attempt to evaluate IdentifierNode",
                node.getLocation());
        return null; // Never reached
    }

    /**
     * Expression evaluation for a variable - resolve variable from the frame
     */
    public Value visitVariableNode(ExpNode.VariableNode node) {
        beginExec("Variable");
        SymEntry.VarEntry entry = node.getVariable();
        /* Construct the variable's address from its static level and offset */
        Value lValue = new AddressValue(entry.getLevel(), entry.getOffset());
        endExec("Variable");
        return lValue;
    }

    /**
     * Expression evaluation for a binary operator expression
     **/
    public Value visitBinaryNode(ExpNode.BinaryNode node) {
        beginExec("Binary");
        int result = -1;
        /* Evaluate the left and right sides of the operator expression */
        int left = node.getLeft().evaluate(this).getInteger();
        int right = node.getRight().evaluate(this).getInteger();
        /* Perform the operation on the left and right side of the expression */
        switch (node.getOp()) {
            /* Mathematical operations */
            case ADD_OP:
                result = left + right;
                break;
            case SUB_OP:
                result = left - right;
                break;
            case MUL_OP:
                result = left * right;
                break;
            case DIV_OP:
                /* Error when division by zero occurs */
                if (right == 0) {
                    runtime("Division by zero", node.getRight().getLocation(),
                            currentFrame);
                }
                result = left / right;
                break;
            /* Logical operations - resulting in 1 for true and 0 for false */
            case EQUALS_OP:
                result = (left == right ? Type.TRUE_VALUE : Type.FALSE_VALUE);
                break;
            case NEQUALS_OP:
                result = (left != right ? Type.TRUE_VALUE : Type.FALSE_VALUE);
                break;
            case GREATER_OP:
                result = (left > right ? Type.TRUE_VALUE : Type.FALSE_VALUE);
                break;
            case LESS_OP:
                result = (left < right ? Type.TRUE_VALUE : Type.FALSE_VALUE);
                break;
            case LEQUALS_OP:
                result = (left <= right ? Type.TRUE_VALUE : Type.FALSE_VALUE);
                break;
            case GEQUALS_OP:
                result = (left >= right ? Type.TRUE_VALUE : Type.FALSE_VALUE);
                break;
            case INVALID_OP:
            default:
                errors.fatal("PL0 Internal error: Unknown operator",
                        node.getLocation());
        }
        endExec("Binary");
        return new IntegerValue(result);
    }

    /**
     * Expression evaluation for a unary operator expression
     **/
    public Value visitUnaryNode(ExpNode.UnaryNode node) {
        beginExec("Unary");
        /* Handle unary operators */
        int result = node.getArg().evaluate(this).getInteger();
        //noinspection SwitchStatementWithTooFewBranches
        switch (node.getOp()) {
            case NEG_OP:
                result = -result;
                break;
            default:
                // Never reached
                errors.fatal("PL0 Internal error: Unknown operator",
                        node.getLocation());
        }
        endExec("Unary");
        return new IntegerValue(result);
    }

    /**
     * Expression evaluation for dereference - evaluate subexpression
     */
    public Value visitDereferenceNode(ExpNode.DereferenceNode node) {
        beginExec("Dereference");
        Value lValue = node.getLeftValue().evaluate(this);
        /* Resolve the frame containing the variable node */
        Frame frame = currentFrame.lookupFrame(lValue.getAddressLevel());
        /* Retrieve the variables value from the frame */
        Value result = frame.lookup(lValue.getAddressOffset());
        if (result == null) {
            runtime("variable accessed before assignment", node.getLocation(),
                    currentFrame);
            return null; // Never reached
        }
        endExec("Dereference");
        return result;
    }

    /**
     * Expression evaluation for narrow subrange - perform subrange bound check
     */
    public Value visitNarrowSubrangeNode(ExpNode.NarrowSubrangeNode node) {
        beginExec("NarrowSubrange");
        Value val = node.getExp().evaluate(this);
        Type.SubrangeType subrange = node.getSubrangeType();

        /* Perform a subrange bounds check for the value */
        if (!subrange.containsElement(subrange.getBaseType(), val.getInteger())) {
            runtime("bounds check failed at line "
                    + node.getLocation().getLine() + ": " + val + " not in "
                    + subrange, node.getLocation(), currentFrame);
        }
        endExec("NarrowSubrange");
        return val;
    }

    /**
     * Expression evaluation for widen subrange - evaluate subexpression
     */
    public Value visitWidenSubrangeNode(ExpNode.WidenSubrangeNode node) {
        beginExec("WidenSubrange");
        Value result = node.getExp().evaluate(this);
        endExec("WidenSubrange");
        return result;
    }

    /* Supporting Methods */

    /**
     * Signal a runtime error has occurred at a given location
     */
    private void runtime(String m, Location loc, Frame frame) {
        String error = m + System.lineSeparator() + frame.toString();
        errors.fatal(error, loc);
    }

    /**
     * Push current node onto debug rule stack and increase debug level
     */
    private void beginExec(String node) {
        debug.beginDebug(node);
    }

    /**
     * Pop current node from debug rule stack and decrease debug level
     */
    private void endExec(String node) {
        debug.endDebug(node);
    }
}
