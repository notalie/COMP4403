package tree;

import java.util.*;

import com.sun.tools.internal.jxc.ap.Const;
import java_cup.runtime.ComplexSymbolFactory.Location;
import syms.SymEntry;
import syms.Type;

/**
 * class ExpNode - Abstract Syntax Tree representation of expressions.
 * Abstract class representing expressions.
 * Actual expression nodes extend ExpNode.
 * All expression nodes have a location and a type.
 */
public abstract class ExpNode {
    /**
     * Location in the source code of the expression
     */
    private final Location loc;
    /**
     * Type of the expression (usually determined by static checker)
     */
    protected Type type;

    /**
     * Constructor when type is known
     */
    private ExpNode(Location loc, Type type) {
        this.loc = loc;
        this.type = type;
    }

    /**
     * Constructor when type as yet unknown
     */
    private ExpNode(Location loc) {
        this(loc, Type.ERROR_TYPE);
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Location getLocation() {
        return loc;
    }

    /**
     * Each subclass of ExpNode must provide a transform method
     * to do type checking and transform the expression node to
     * handle type coercions, etc.
     *
     * @param visitor object that implements a traversal.
     * @return transformed expression node
     */
    public abstract ExpNode transform(ExpTransform<ExpNode> visitor);


    /**
     * Each subclass of ExpNode must provide a genCode method
     * to visit the expression node to handle code generation.
     *
     * @param visitor object that implements a traversal.
     * @return generated code
     */
    public abstract Code genCode(ExpTransform<Code> visitor);

    /**
     * Tree node representing an erroneous expression.
     */
    public static class ErrorNode extends ExpNode {

        public ErrorNode(Location loc) {
            super(loc, Type.ERROR_TYPE);
        }

        @Override
        public ExpNode transform(ExpTransform<ExpNode> visitor) {
            return visitor.visitErrorExpNode(this);
        }

        @Override
        public Code genCode(ExpTransform<Code> visitor) {
            return visitor.visitErrorExpNode(this);
        }

        @Override
        public String toString() {
            return "ErrorNode";
        }
    }

    /**
     * Tree node representing a constant within an expression.
     */
    public static class ConstNode extends ExpNode {
        /**
         * constant's value
         */
        private final int value;

        public ConstNode(Location loc, Type type, int value) {
            super(loc, type);
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        @Override
        public ExpNode transform(ExpTransform<ExpNode> visitor) {
            return visitor.visitConstNode(this);
        }

        @Override
        public Code genCode(ExpTransform<Code> visitor) {
            return visitor.visitConstNode(this);
        }

        @Override
        public String toString() {
            return Integer.toString(value);
        }
    }

    /**
     * Identifier node is used until the identifier can be resolved
     * to be either a constant or a variable during the static
     * semantics checking phase.
     */
    public static class IdentifierNode extends ExpNode {
        /**
         * Name of the identifier
         */
        private final String id;

        public IdentifierNode(Location loc, String id) {
            super(loc);
            this.id = id;
        }

        public String getId() {
            return id;
        }

        @Override
        public ExpNode transform(ExpTransform<ExpNode> visitor) {
            return visitor.visitIdentifierNode(this);
        }

        @Override
        public Code genCode(ExpTransform<Code> visitor) {
            return visitor.visitIdentifierNode(this);
        }

        @Override
        public String toString() {
            return "IdentifierNode(" + id + ")";
        }
    }

    /**
     * Tree node representing a variable.
     */
    public static class VariableNode extends ExpNode {
        /**
         * Symbol table entry for the variable
         */
        final SymEntry.VarEntry variable;

        public VariableNode(Location loc, SymEntry.VarEntry variable) {
            super(loc, variable.getType());
            this.variable = variable;
        }

        public SymEntry.VarEntry getVariable() {
            return variable;
        }

        @Override
        public ExpNode transform(ExpTransform<ExpNode> visitor) {
            return visitor.visitVariableNode(this);
        }

        @Override
        public Code genCode(ExpTransform<Code> visitor) {
            return visitor.visitVariableNode(this);
        }

        @Override
        public String toString() {
            return variable.getIdent();
        }
    }

    /**
     * Tree node for a binary operator
     */
    public static class BinaryNode extends ExpNode {
        /**
         * Binary operator
         */
        private Operator op;
        /**
         * Arguments for operator
         */
        ExpNode left, right;

        public BinaryNode(Location loc, Operator op, ExpNode left, ExpNode right) {
            super(loc);
            this.op = op;
            this.left = left;
            this.right = right;
        }

        public Operator getOp() {
            return op;
        }

        public void setOp(Operator op) {
            this.op = op;
        }

        public ExpNode getLeft() {
            return left;
        }

        public void setLeft(ExpNode left) {
            this.left = left;
        }

        public ExpNode getRight() {
            return right;
        }

        public void setRight(ExpNode right) {
            this.right = right;
        }

        @Override
        public ExpNode transform(ExpTransform<ExpNode> visitor) {
            return visitor.visitBinaryNode(this);
        }

        @Override
        public Code genCode(ExpTransform<Code> visitor) {
            return visitor.visitBinaryNode(this);
        }

        @Override
        public String toString() {
            return op + "(" + left + "," + right + ")";
        }
    }

    /**
     * Tree node for a unary operator
     */
    public static class UnaryNode extends ExpNode {
        /**
         * Unary operator
         */
        private Operator op;
        /**
         * Argument for operator
         */
        private ExpNode arg;

        public UnaryNode(Location loc, Operator op, ExpNode arg) {
            super(loc);
            this.op = op;
            this.arg = arg;
        }

        public Operator getOp() {
            return op;
        }

        public void setOp(Operator op) {
            this.op = op;
        }

        public ExpNode getArg() {
            return arg;
        }

        public void setArg(ExpNode arg) {
            this.arg = arg;
        }

        @Override
        public ExpNode transform(ExpTransform<ExpNode> visitor) {
            return visitor.visitUnaryNode(this);
        }

        @Override
        public Code genCode(ExpTransform<Code> visitor) {
            return visitor.visitUnaryNode(this);
        }

        @Override
        public String toString() {
            return op + "(" + arg + ")";
        }

    }
    /**
     * Tree node for the coercion representing a dereference of an LValue.
     * A Dereference node references an ExpNode node and represents the
     * dereference of the "address" given by the leftValue to give the value
     * at that address. The type of the leftValue must be a ReferenceType.
     */
    public static class DereferenceNode extends ExpNode {
        /**
         * LValue to dereference
         */
        private ExpNode leftValue;

        /* The type of the Dereference node is the base type of the type
         * of the expression being dereferenced.
         */
        public DereferenceNode(ExpNode exp) {
            super(exp.getLocation(), exp.getType().optDereferenceType());
            assert exp.getType() instanceof Type.ReferenceType;
            this.leftValue = exp;
        }

        public ExpNode getLeftValue() {
            return leftValue;
        }

        public void setLeftValue(ExpNode leftValue) {
            this.leftValue = leftValue;
        }

        @Override
        public ExpNode transform(ExpTransform<ExpNode> visitor) {
            return visitor.visitDereferenceNode(this);
        }

        @Override
        public Code genCode(ExpTransform<Code> visitor) {
            return visitor.visitDereferenceNode(this);
        }

        @Override
        public String toString() {
            return "Dereference(" + leftValue + ")";
        }
    }

    /**
     * Tree node for the coercion representing a dereference of an LValue.
     * A Dereference node references an ExpNode node and represents the
     * dereference of the "address" given by the leftValue to give the value
     * at that address. The type of the leftValue must be a ReferenceType.
     */
    public static class PointerNode extends ExpNode {
        /**
         * LValue to dereference
         */
        private ExpNode leftValue;

        /* The type of the Dereference node is the base type of the type
         * of the expression being dereferenced.
         */
        public PointerNode(ExpNode exp) {
            super(exp.getLocation(), exp.getType().optDereferenceType());
            this.leftValue = exp;
        }

        public ExpNode getLeftValue() {
            return leftValue;
        }

        public void setLeftValue(ExpNode leftValue) {
            this.leftValue = leftValue;
        }

        @Override
        public ExpNode transform(ExpTransform<ExpNode> visitor) {
            return visitor.visitPointerNode(this);
        }

        @Override
        public Code genCode(ExpTransform<Code> visitor) {
            return visitor.visitPointerNode(this);
        }

        @Override
        public String toString() {
            return "Dereference(" + leftValue + ")";
        }
    }

    /**
     * Node created when 'new' is called
     */
    public static class NewNode extends ExpNode {

        private Type type;

        public NewNode(Location loc, Type type) {
            super(loc, new Type.PointerType(loc, new Type.PointerType(loc, type)));
            this.type = type;
        }

        public Type getNewNodeType() {
            return this.type;
        }

        @Override
        public ExpNode transform(ExpTransform<ExpNode> visitor) { return visitor.visitNewNode(this); }

        @Override
        public Code genCode(ExpTransform<Code> visitor) {
            return visitor.visitNewNode(this);
        }

        @Override
        public Location getLocation() {return super.getLocation();}
    }

    /**
     * A reference node that will get the value of a record when `x.y` is called.
     * It will not return the address unless the `x` is a pointer to a record type
     */
    public static class ReferenceNode extends ExpNode {
        /**
         * LValue to reference
         */
        private ExpNode leftValue;

        /**
         * RValue to reference
         */
        private String id;

        /* The type of the Reference node is the base type of the type
         * of the expression being referenced.
         */
        public ReferenceNode(ExpNode exp, String id) {
            super(exp.getLocation(), exp.getType().optDereferenceType());
            this.leftValue = exp;
            this.id = id;
        }

        public ExpNode getLeftValue() {
            return leftValue;
        }

        public String getId() {
            return id;
        }

        public void setLeftValue(ExpNode leftValue) {
            this.leftValue = leftValue;
        }

        @Override
        public ExpNode transform(ExpTransform<ExpNode> visitor) {
            return visitor.visitReferenceNode(this);
        }

        @Override
        public Code genCode(ExpTransform<Code> visitor) {
            return visitor.visitReferenceNode(this);
        }

        @Override
        public String toString() {
            return "Reference(" + leftValue + "." + id + ")";
        }

    }

    /**
     * A reference node that will get the value of a record when `x.y` is called.
     * It will not return the address unless the `x` is a pointer to a record type
     */
    public static class RecordNode extends ExpNode {
        /**
         * LValue to reference
         */
        private  List<ExpNode> expList;

        /* The type of the Reference node is the base type of the type
         * of the expression being referenced.
         */
        public RecordNode(Location loc, Type type, List<ExpNode> expList) {
            super(loc, type);
            this.type = type;
            this.expList = expList;
        }

        public List<ExpNode> getExpList() {
            return this.expList;
        }

        public void setExpList( List<ExpNode> newList) {
            this.expList = newList;
        }

        @Override
        public ExpNode transform(ExpTransform<ExpNode> visitor) {
            return visitor.visitRecordNode(this);
        }

        @Override
        public Code genCode(ExpTransform<Code> visitor) {
            return visitor.visitRecordNode(this);
        }

        @Override
        public String toString() {
            return "Record{" + this.expList + "}";
        }

    }

    /**
     * Tree node representing a coercion that narrows a subrange
     */
    public static class NarrowSubrangeNode extends ExpNode {
        /**
         * Expression to be narrowed
         */
        private ExpNode exp;

        /* @requires exp.getType().equals(type.getBaseType()) */
        public NarrowSubrangeNode(Type.SubrangeType type, ExpNode exp) {
            super(exp.getLocation(), type);
            assert type.getBaseType() == Type.ERROR_TYPE ||
                    exp.getType().equals(type.getBaseType());
            this.exp = exp;
        }

        public Type.SubrangeType getSubrangeType() {
            return (Type.SubrangeType) getType();
        }

        public ExpNode getExp() {
            return exp;
        }

        @Override
        public ExpNode transform(ExpTransform<ExpNode> visitor) {
            return visitor.visitNarrowSubrangeNode(this);
        }

        @Override
        public Code genCode(ExpTransform<Code> visitor) {
            return visitor.visitNarrowSubrangeNode(this);
        }

        @Override
        public String toString() {
            return "NarrowSubrange(" + exp + ":" + type + ")";
        }
    }

    /**
     * Tree node representing a widening of a subrange
     */
    public static class WidenSubrangeNode extends ExpNode {
        /**
         * Expression to be widened
         */
        private final ExpNode exp;

        /* @requires exp.getType() instanceof Type.SubrangeType */
        public WidenSubrangeNode(ExpNode exp) {
            super(exp.getLocation(), ((Type.SubrangeType) exp.getType()).getBaseType());
            this.exp = exp;
        }

        public ExpNode getExp() {
            return exp;
        }

        @Override
        public ExpNode transform(ExpTransform<ExpNode> visitor) {
            return visitor.visitWidenSubrangeNode(this);
        }

        @Override
        public Code genCode(ExpTransform<Code> visitor) {
            return visitor.visitWidenSubrangeNode(this);
        }

        @Override
        public String toString() {
            return "WidenSubrange(" + exp + ":" + getType() + ")";
        }
    }

}
