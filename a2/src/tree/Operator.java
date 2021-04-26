package tree;

/**
 * enumeration Operator - operators in abstract syntax tree.
 */
public enum Operator {
    /* Binary operators */
    ADD_OP("_+_"),
    SUB_OP("_-_"),
    MUL_OP("_*_"),
    DIV_OP("_/_"),
    EQUALS_OP("_=_"),
    NEQUALS_OP("_!=_"),
    GREATER_OP("_>_"),
    LESS_OP("_<_"),
    LEQUALS_OP("_<=_"),
    GEQUALS_OP("_>=_"),
    /* unary operators */
    NEG_OP("-_", 1),
    INVALID_OP("INVALID");

    /**
     * The name of the operator
     */
    private final String name;
    /**
     * The amount of arguments of the operator
     */
    private final int args;

    Operator(String name) {
        this(name, 2);
    }

    Operator(String name, int args) {
        this.name = name;
        this.args = args;
    }

    public String getName() {
        return name;
    }

    public int getArgCount() {
        return args;
    }

    public String toString() {
        return name;
    }
}
