package syms;

import source.ErrorHandler;
import java_cup.runtime.ComplexSymbolFactory.Location;
import syms.Type.ReferenceType;
import tree.ConstExp;
import tree.Operator;
import tree.StatementNode;

/**
 * This class provides the individual entries that go in a symbol table.
 * The kinds of entries are constants, types, variables and procedures.
 * It provides subclasses for each of these kinds.
 * All entries have an identifier, the location (in the source input)
 * of the definition of the identifier, a scope in which it was declared
 * and a type.
 * The declared type may be a type identifier which needs to be resolved
 * (looked up in the symbol table) to get its corresponding real type.
 * Resolving types is a little tricky because the syntax does not rule out
 * circular references.
 * Particular kinds of entries have additional fields.
 */
public abstract class SymEntry {
    /**
     * Name of the entry
     */
    private final String ident;
    /**
     * location of declaration in source input
     */
    final Location loc;
    /**
     * scope in which declared - set when the entry is added to a scope
     */
    Scope scope;
    /**
     * type of the identifier after type resolution
     */
    Type type;
    /**
     * whether id has been resolved and space allocated where necessary
     */
    boolean resolved;
    /* All entries have all the above fields. */

    /**
     * Only subclasses of SymEntry have public constructors.
     */
    private SymEntry(String ident, Location loc, Type type, boolean resolved) {
        // Note that scope is initially null and updated when added to a scope
        this.ident = ident;
        this.loc = loc;
        this.type = type;
        this.resolved = resolved;
    }

    public String getIdent() {
        return ident;
    }

    public Location getLocation() {
        return loc;
    }

    public void setScope(Scope scope) {
        this.scope = scope;
    }

    public int getLevel() {
        return scope.getLevel();
    }

    /**
     * @return the type of the entry.
     * May need to resolve type entry first.
     */
    public Type getType() {
        if (!resolved) {
            resolve();
        }
        return type;
    }

    /**
     * Resolve any references to type identifiers in supplied scope
     */
    public void resolve() {
        if (!resolved) {
            type = type.resolveType();
            resolved = true;
        }
    }

    /**
     * For debugging
     */
    String toString(String kind, String sep) {
        return kind + " " + ident + sep + type +
                (scope == null ? "" : " level " + scope.getLevel());
    }

    /**
     * Symbol table entry for a CONST identifier
     */
    public static class ConstantEntry extends SymEntry {
        /**
         * The value of the constant represented as an integer.
         */
        int value;
        /**
         * Expression tree for constant before it is evaluated
         */
        ConstExp tree;

        /**
         * Status of constant for resolving references
         */
        private enum Status {Unresolved, Resolving, Resolved}

        /**
         * Resolved if expression has been evaluated
         */
        Status status;

        /**
         * Constructor if the constant value (and hence its type) is known
         */
        ConstantEntry(String id, Location loc, Type t, int val) {
            super(id, loc, t, true);
            value = val;
            status = Status.Resolved;
        }

        /**
         * Constructor when only an abstract syntax tree is available.
         */
        ConstantEntry(String id, Location loc, Type t, ConstExp val) {
            super(id, loc, t, false);
            value = 0x80808080;    // silly default value
            tree = val;
            status = Status.Unresolved;
        }

        /**
         * Resolve references to constant identifiers and evaluate expression
         */
        @Override
        public void resolve() {
            switch (status) {
                case Unresolved:
                    status = Status.Resolving;
                    value = tree.getValue();
                    type = tree.getType();
                    status = Status.Resolved;
                    resolved = true;
                    break;
                case Resolving:
                    error("circular reference in constant expression", loc);
                    status = Status.Resolved;
                    resolved = true;
                    break;
                case Resolved:
                    break;
            }
        }

        public int getValue() {
            if (!resolved) {
                resolve();
            }
            return value;
        }

        @Override
        public String toString() {
            return toString("CONST ", " : ") + " = " + value;
        }
    }

    /**
     * Symbol table entry for a TYPE identifier
     */
    public static class TypeEntry extends SymEntry {

        TypeEntry(String id, Location loc, Type t) {
            super(id, loc, t, false);
        }

        @Override
        public String toString() {
            return toString("TYPE  ", " = ");
        }
    }

    /**
     * Symbol table entry for a variable identifier
     */
    public static class VarEntry extends SymEntry {
        /**
         * offset of variable starting from 0
         */
        int offset;

        public VarEntry(String id, Location loc, Type t) {
            super(id, loc, new ReferenceType(t), false);
        }

        public ReferenceType getType() {
            return (ReferenceType) super.getType();
        }

        /**
         * resolving a variable requires allocating space for it
         */
        @Override
        public void resolve() {
            if (!resolved) {
                // resolve the type of the variable
                super.resolve();
                /* Space is allocated for the variable and the address of that
                 * location placed in the entry for the variable.
                 * The space allocated depends on the size of its type.
                 */
                Type baseType = ((Type.ReferenceType) type).getBaseType();
                offset = scope.allocVariableSpace(baseType.getSpace());
            }
        }

        /**
         * requires resolved
         */
        public int getOffset() {
            assert resolved;
            return offset;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }

        @Override
        public String toString() {
            return toString("VAR   ", " : ") + " offset " + offset;
        }
    }

    /**
     * Symbol table entry for a procedure identifier
     */
    public static class ProcedureEntry extends SymEntry {
        /**
         * Body of the procedure
         */
        private StatementNode.BlockNode block;
        /**
         * Scope of entries declared locally to the procedure
         */
        private Scope localScope;

        public ProcedureEntry(String id, Location loc,
                              Type.ProcedureType type) {
            super(id, loc, type, false);
        }

        public ProcedureEntry(String id, Location loc) {
            this(id, loc, new Type.ProcedureType(loc));
        }

        @Override
        public Type.ProcedureType getType() {
            return (Type.ProcedureType) type;
        }

        public Scope getLocalScope() {
            return localScope;
        }

        public void setLocalScope(Scope localScope) {
            this.localScope = localScope;
        }

        public StatementNode.BlockNode getBlock() {
            return block;
        }

        public void setBlock(StatementNode.BlockNode block) {
            this.block = block;
        }

        @Override
        public String toString() {
            return toString("PROC  ", " : ");
        }
    }

    /**
     * Symbol table entry for an operator
     */
    public static class OperatorEntry extends SymEntry {
        /**
         * Operator entry constructor with single type
         */
        OperatorEntry(String id, Location loc, Type operatorType) {
            super(id, loc, operatorType, true);
        }

        /**
         * Add an extra type for an operator.
         */
        void extendType(Type operatorType) {
            if (type instanceof Type.IntersectionType) {
                /* Extend type by adding operatorType */
                ((Type.IntersectionType) type).addType(operatorType);
            } else {
                /* Convert type to intersection type containing both
                 * current type and operatorType.
                 */
                type = new Type.IntersectionType(loc, type, operatorType);
            }
        }

        @Override
        public String toString() {
            return toString("Operator  ", " : ");
        }
    }

    private static void error(String message, Location loc) {
        ErrorHandler.getErrorHandler().error(message, loc);
    }
}
