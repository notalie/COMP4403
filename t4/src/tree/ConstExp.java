package tree;

import source.ErrorHandler;
import source.Errors;
import java_cup.runtime.ComplexSymbolFactory.Location;
import syms.Predefined;
import syms.SymEntry;
import syms.Scope;
import syms.Type;

/**
 * Constant expressions tree structures used to evaluate
 * compile-time constant expressions.
 */
public abstract class ConstExp {
    /**
     * Location in the source code of the expression
     */
    final Location loc;

    /**
     * Status of constant for resolving references
     */
    private enum Status {Unresolved, Resolving, Resolved}

    /**
     * Resolved if expression has been evaluated
     */
    Status status;
    /**
     * Type of the expression
     */
    Type type;
    /**
     * Value of the expression
     */
    int value;
    /**
     * Error handler
     */
    final Errors errors = ErrorHandler.getErrorHandler();

    /**
     * Constructor used by subclass constructors only
     */
    ConstExp(Location loc, Status status, Type type, int value) {
        this.loc = loc;
        this.status = status;
        this.type = type;
        this.value = value;
    }

    /**
     * Constructor for an initially unevaluated expression
     */
    ConstExp(Location loc, Status status) {
        /* Defaults to error type and silly value */
        this(loc, status, Type.ERROR_TYPE, 0x80808080);
    }

    public Type getType() {
        if (status == Status.Unresolved) {
            evaluate();
        }
        return type;
    }

    public int getValue() {
        if (status == Status.Unresolved) {
            evaluate();
        }
        return value;
    }

    /**
     * Evaluate a constant and store value and type.
     * Overridden in sub-types where necessary.
     */
    void evaluate() {
        // Default for expression that does not need evaluating.
    }

    /**
     * For handling erroneous constant expressions
     */
    public static class ErrorNode extends ConstExp {

        public ErrorNode(Location loc) {
            super(loc, Status.Resolved);
        }
    }

    /**
     * A constant expression consisting of a number
     */
    public static class NumberNode extends ConstExp {

        public NumberNode(Location loc, Type type, int value) {
            super(loc, Status.Resolved, type, value);
        }
    }

    /**
     * A constant expression consisting of a negated constant expression
     */
    public static class NegateNode extends ConstExp {
        private final ConstExp subExp;

        public NegateNode(Location loc, ConstExp subExp) {
            super(loc, Status.Unresolved);
            this.subExp = subExp;
        }

        @Override
        protected void evaluate() {
            type = subExp.getType();
            if (type != Predefined.INTEGER_TYPE) {
                errors.error("can only negate an integer", loc);
            } else {
                value = -subExp.getValue();
            }
            status = Status.Resolved;
        }
    }

    /**
     * A constant expression consisting of a reference to an identifier
     */
    public static class ConstIdNode extends ConstExp {
        private final String id;    /**
         * Scope in which expression should be evaluated
         */
        private final Scope scope;


        public ConstIdNode(Location loc, String id, Scope scope) {
            super(loc, Status.Unresolved);
            this.id = id;
            this.scope = scope;
        }

        /**
         * In evaluating a constant expression consisting of an identifier
         * we need to be careful in case it is circularly defined.
         * To handle this we set its status to Resolving while its value
         * is being calculated, so that if during the calculation we try
         * to calculate the value of the same identifier again, we report an
         * error.
         */
        @Override
        protected void evaluate() {
//          System.out.println( " ConstExp Resolving " + id + " " + status );
            switch (status) {
                case Unresolved:
                    status = Status.Resolving;
                    SymEntry entry = scope.lookup(id);
                    if (entry instanceof SymEntry.ConstantEntry) {
                        SymEntry.ConstantEntry constEntry =
                                (SymEntry.ConstantEntry) entry;
                        type = constEntry.getType();
                        value = constEntry.getValue();
                        status = Status.Resolved;
                    } else {
                        errors.error("Constant identifier expected", loc);
                    }
                    break;
                case Resolving:
                    errors.error(id + " is circularly defined", loc);
                    /* Will resolve to error type and silly value.
                     * Set to Resolved to avoid repeated attempts to
                     * resolve the unresolvable, and hence avoid
                     * unnecessary repeated error messages. */
                    status = Status.Resolved;
                    break;
                case Resolved:
                    /* Already resolved */
                    break;
            }
        }
    }
}
