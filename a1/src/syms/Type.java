package syms;

import java.util.*;

import source.ErrorHandler;
import source.Errors;
import java_cup.runtime.ComplexSymbolFactory.Location;
import tree.ConstExp;
import tree.ExpNode;
import tree.Operator;

/**
 * This class provides the type structures defining the types
 * available in the language.
 * It provides subclasses for each of the different kinds of "type",
 * e.g., scalar types, subrange types, products of types, function types,
 * intersections of types, reference types, and the procedure types.
 * IdRefType is provided to allow type names to be used as types.
 * <p>
 * As well as the constructors for the types it provides a number of
 * access methods.
 * Each type provides a method for coercing an expression to the type.
 * Type also provides the special type ERROR_TYPE,
 * which is used for handling type errors.
 */
public abstract class Type {
    /**
     * Value used for false
     */
    public static int FALSE_VALUE = 0;
    /**
     * Value used for true
     */
    public static int TRUE_VALUE = 1;
    /**
     * Size of an integer variable
     */
    public static final int SIZE_OF_INT = 1;
    /**
     * Size of a boolean variable
     */
    public static int SIZE_OF_BOOLEAN = 1;
    /**
     * Size of an address (reference)
     */
    public static final int SIZE_OF_ADDRESS = 1;

    /**
     * All types require space to be allocated (may be 0)
     */
    int space;

    /**
     * Track whether type has been resolved
     */
    boolean resolved;

    /**
     * Name of type for error messages and for the name in IdRefTypes
     */
    protected String name;

    /**
     * Location of reference to type
     */
    final Location loc;

    /**
     * Error handler
     */
    private static final Errors errors = ErrorHandler.getErrorHandler();

    /**
     * Basic constructor for Type.
     * Only subclasses provide public constructors.
     * @param loc location of the definition of the type.
     * @param space size of the type at run time.
     * @param resolved true iff type identifiers in the type have been resolved.
     */
    private Type(Location loc, int space, boolean resolved) {
        this.loc = loc;
        this.space = space;
        this.resolved = resolved;
    }
    /**
     * Constructor when the type is not initially resolved.
     */
    private Type(Location loc, int space) {
        this(loc, space, false);
    }

    /**
     * Constructor when the type has a name
     */
    private Type(Location loc, int space, boolean resolved, String name) {
        this(loc, space, resolved);
        this.name = name;
    }

    /**
     * @return the space required for an element of the type.
     * requires that the type has been resolved
     */
    public int getSpace() {
        assert resolved;
        return space;
    }

    /**
     * Most types have a name but not all
     */
    public String getName() {
        if (name == null) {
            return this.toString();
        } else {
            return name;
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }

    /**
     * Resolve identifier references anywhere within type.
     * Default just sets resolved true; it needs to be overridden
     * when appropriate within subclasses of Type.
     */
    public Type resolveType() {
        resolved = true;
        return this;
    }

    /**
     * For a scalar type, check if a constant of type constType
     * is compatible and, if so, that value is an element of the scalar type
     * The default method for non-scalar types returns false.
     *
     * @param constType type of the constant (int or boolean)
     * @param value     of the constant
     * @return true iff a compatible type and value is an element
     */
    public boolean containsElement(Type constType, int value) {
        return false;
    }

    /**
     * The coercion methods will throw an IncompatibleTypes exception
     * if the expression being coerced cannot be coerced to 'this' type.
     */
    public static class IncompatibleTypes extends Exception {
        /**
         * Generated serialization identifier
         */
        private static final long serialVersionUID = 7538092667013471204L;
        final Location loc;

        /**
         * Constructor.
         *
         * @param msg error message to be reported
         * @param loc location of the expression for error reporting
         */
        IncompatibleTypes(String msg, Location loc) {
            super(msg);
            this.loc = loc;
        }

        Location getLocation() {
            return loc;
        }
    }

    /**
     * Coerce an expression to 'this' type and report error if incompatible
     *
     * @param exp is the expression to be coerced
     * @return the coerced expression or ErrorNode on failure
     */
    public ExpNode coerceExp(ExpNode exp) {
        /* Try coercing the expression. */
        try {
            return this.coerceToType(exp);
        } catch (IncompatibleTypes e) {
            /* At this point the coercion has failed. */
            errors.debugMessage("******" + e.getMessage());
            errors.error(e.getMessage(), e.getLocation());
            return new ExpNode.ErrorNode(e.getLocation());
        }
    }

    /**
     * Coerce exp to 'this' type or throw IncompatibleTypes exception if it cannot.
     * This method handles the general case and calls the method coerce on
     * 'this' type to handle each subtype's particular coercion rules.
     *
     * @param exp expression to be coerced
     * @return coerced expression
     * @throws IncompatibleTypes if cannot coerce to 'this' type
     */
    public ExpNode coerceToType(ExpNode exp) throws IncompatibleTypes {
        errors.debugMessage("Coercing " + exp + ":" + exp.getType().getName() +
                " to " + this.getName());
        errors.incDebug();
        ExpNode newExp = exp;
        /* Unless this type is a reference type, optionally dereference
         * the expression to get its base type.
         */
        if (!(this instanceof ReferenceType)) {
            newExp = optDereferenceExp(newExp);
        }
        Type fromType = newExp.getType();
        /* No need to coerce if already the same type or
         * fromType is ERROR_TYPE and hence an error message has already been given.
         */
        if (!this.equals(fromType) && fromType != ERROR_TYPE) {
            /* Try coercing the expression. Dynamic dispatch on the desired
             * type is used to control the coercion process.
             */
            try {
                newExp = this.coerce(newExp);
            } catch (IncompatibleTypes e) {
                errors.debugMessage("Failed to coerce " + newExp + " to " +
                        this.getName());
                errors.decDebug();
                /* Throw an error to allow the caller to decide whether an error
                 * message needs to be generated.
                 */
                throw e;
            }
        }
        errors.debugMessage("Succeeded");
        errors.decDebug();
        return newExp;
    }

    /**
     * Coerce an expression node to be of 'this' type.
     * This default version just throws an exception.
     * Subclasses of Type override this method.
     *
     * @param exp expression to be coerced
     * @return resulting coerced expression node.
     * @throws IncompatibleTypes exception if it cannot coerce.
     */
    ExpNode coerce(ExpNode exp) throws IncompatibleTypes {
        throw new IncompatibleTypes(
                "cannot treat " + exp.getType() + " as " + this,
                exp.getLocation());
    }

    /**
     * Type equality. Overridden for most subclasses.
     *
     * @param other - type to be compared with 'this' type.
     */
    public boolean equals(Type other) {
        return this == other;
    }

    /**
     * Add operators for the type -- overridden by types that need to add operators
     */
    public void addOperators(Scope scope) {}

    /**
     * ERROR_TYPE
     * If something is of type ErrorType, an error message for it will already
     * have been issued and hence to avoid generating spurious error messages
     * ERROR_TYPE is compatible with everything.
     */
    public static final Type ERROR_TYPE =
            new Type(ErrorHandler.NO_LOCATION, 0, true, "error_type") {

                @Override
                protected ExpNode coerce(ExpNode exp) {
                    return exp;
                }
            };

    //********************* SCALAR TYPES

    /**
     * If ScalarType then cast to ScalarType and return
     * else return null. Overridden in ScalarType.
     */
    public ScalarType getScalarType() {
        return null;
    }

    /**
     * Scalar types are simple unstructured types that just have a range of
     * possible values. int and boolean are scalar types
     */
    public static class ScalarType extends Type {
        /**
         * lower and upper bounds of scalar type
         */
        int lower;
        int upper;

        /**
         * Basic scalar type constructor
         * @param loc of the definition of the type
         * @param size number of words required for the type at runtime
         * @param lower least value of 'this' type
         * @param upper greatest value of 'this' type
         */
        private ScalarType(Location loc, int size, int lower, int upper) {
            super(loc, size, true);
            this.lower = lower;
            this.upper = upper;
        }

        /** Constructor if the type as a name */
        private ScalarType(Location loc, String name, int size, int lower, int upper) {
            this(loc, size, lower, upper);
            this.name = name;
        }

        /** Predefined types do not have a location */
        public ScalarType(String name, int size, int lower, int upper) {
            this(ErrorHandler.NO_LOCATION, name, size, lower, upper);
        }

        /**
         * Constructor when bounds evaluated later
         */
        private ScalarType(Location loc, int size) {
            super(loc, size);
        }

        /**
         * The least element of the type
         * requires resolved
         */
        public int getLower() {
            assert resolved;
            return lower;
        }

        /**
         * The greatest element of the type
         * requires resolved
         */
        public int getUpper() {
            assert resolved;
            return upper;
        }

        @Override
        public ScalarType getScalarType() {
            return this;
        }

        /**
         * For a scalar type, check if a constant of type constType
         * is compatible and, if so, that value is an element of the scalar type
         * The default method for non-scalar types returns false.
         *
         * @param constType type of the constant (e.g. int or boolean)
         * @param value     of the constant
         * @return true iff a compatible type and value is an element
         */
        public boolean containsElement(Type constType, int value) {
            return this.equals(constType) && lower <= value && value <= upper;
        }

        /**
         * Coerce expression to 'this' Scalar type.
         * The objective is to create an expression of the 'this' scalar type
         * from exp. Note that this method is called from coerceToType, which
         * handles optional dereference of a type and the simple cases when
         * the two types are equal or one is error type.
         *
         * @param exp expression to be coerced
         * @return the coerced expression
         * @throws IncompatibleTypes exception if it is not possible to coerce
         *                           exp to 'this' scalar type
         */
        @Override
        protected ExpNode coerce(ExpNode exp) throws IncompatibleTypes {
            Type fromType = exp.getType();
            if (fromType instanceof SubrangeType) {
                /* This code implements Rule Widen subrange.
                 * If the types do not match, the only other possible type
                 * for the expression which can be coerced to 'this' scalar type
                 * is a subrange type, provided its base type matches
                 * 'this' type. If that is the case we insert a WidenSubrangeNode
                 * of 'this' type with the expression as a subtree.
                 */
                Type baseType = ((SubrangeType) fromType).getBaseType();
                if (this.equals(baseType)) {
                    errors.debugMessage("Widened " + fromType.getName() +
                            " to " + baseType.getName());
                    return new ExpNode.WidenSubrangeNode(exp);
                }
            }
            /* Otherwise we report the failure to coerce the expression via
             * an IncompatibleTypes exception.
             */
            throw new IncompatibleTypes("cannot coerce " +
                    exp.getType().getName() + " to " + this.getName(),
                    exp.getLocation());
        }
    }

    //****************** SUBRANGE TYPES

    /**
     * If 'this' is a subrange type widen it to its base type.
     * Overridden in SubrangeType.
     */
    public Type optWidenSubrange() {
        return this;
    }

    /**
     * Types defined as a subrange of a scalar type.
     */
    public static class SubrangeType extends ScalarType {
        /**
         * The base type of the subrange type
         */
        private Type baseType;
        /**
         * Constant expression trees for lower and upper bounds
         * before evaluation
         */
        private final ConstExp lowerExp;
        private final ConstExp upperExp;

        /**
         * Basic constructor for a subrange type.
         * @param loc location of its definition
         * @param lowerExp constant expression for lower bound of subrange
         * @param upperExp constant expression for upper bound of subrange
         */
        public SubrangeType(Location loc, ConstExp lowerExp, ConstExp upperExp) {
            /* On a byte addressed machine, the size could be scaled to
             * just fit the subrange, e.g., a subrange of 0..255
             * might only require 1 byte.
             */
            super(loc, SIZE_OF_INT);
            this.lowerExp = lowerExp;
            this.upperExp = upperExp;
        }
        /** Predefined types do not have a location */
        public SubrangeType(ConstExp lowerExp, ConstExp upperExp) {
            /* On a byte addressed machine, the size could be scaled to
             * just fit the subrange, e.g., a subrange of 0..255
             * might only require 1 byte.
             */
            this(ErrorHandler.NO_LOCATION, lowerExp, upperExp);
        }

        public Type getBaseType() {
            return baseType;
        }

        @Override
        public Type optWidenSubrange() {
            return baseType;
        }

        /**
         * For a scalar type, check if a constant of type constType
         * is compatible and, if so, that value is an element of the scalar type
         * The default method for non-scalar types returns false.
         *
         * @param constType type of the constant (int or boolean)
         * @param value     of the constant
         * @return true iff a compatible type and value is an element
         */
        public boolean containsElement(Type constType, int value) {
            return baseType.equals(constType) && lower <= value && value <= upper;
        }

        /**
         * Coerce expression to 'this' subrange type
         * The objective is to create an expression of the 'this' subrange type
         * from exp. Note that this method is called from coerceToType, which
         * handles optional dereference of a type and the simple cases when
         * the two types are equal or one is error type.
         *
         * @param exp expression to be coerced
         * @return the coerced expression
         * @throws IncompatibleTypes exception if it is not possible to coerce
         *                           exp to 'this' subrange type
         */
        @Override
        protected ExpNode coerce(ExpNode exp) throws IncompatibleTypes {
            /* This implements Rule Narrow subrange in the static semantics.
             * If the types do not match, we can try coercing the expression
             * to the base type of 'this' subrange, and then narrow that
             * to 'this' type. If the coercion to the base type fails it will
             * generate an exception, which is allowed to pass up to the caller.
             */
            ExpNode coerceExp = baseType.coerceToType(exp);
            /* If we get here, coerceExp is of the same type as the base
             * type of 'this' subrange type. We just need to narrow it
             * down to 'this' subrange.
             */
            errors.debugMessage("Narrowed " + exp.getType().getName() +
                    " to " + this.getName());
            return new ExpNode.NarrowSubrangeNode(this, coerceExp);
        }

        /**
         * Resolving a subrange type requires the lower and upper bound
         * expressions to be evaluated.
         */
        @Override
        public Type resolveType() {
            if (!resolved) {
                lower = lowerExp.getValue();
                upper = upperExp.getValue();
                baseType = lowerExp.getType();
                if (!upperExp.getType().equals(lowerExp.getType())) {
                    errors.error("Types of bounds of subrange must match", loc);
                    baseType = ERROR_TYPE; // TODO or leave as lower type?
                } else if (upper < lower) {
                    errors.error("Upper bound of subrange less than lower bound", loc);
                }
                resolved = true;
            }
            return this;
        }

        /**
         * A subrange type is equal to another subrange type only if they have
         * the same base type and lower and upper bounds.
         */
        @Override
        public boolean equals(Type other) {
            if (other instanceof SubrangeType) {
                SubrangeType otherSubrange = (SubrangeType) other;
                return baseType.equals(otherSubrange.getBaseType()) &&
                        lower == otherSubrange.getLower() &&
                        upper == otherSubrange.getUpper();
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return (baseType == null ? "<undefined>" : baseType.getName()) +
                    "[" + lower + ".." + upper + "]";
        }
    }


    //********************* PRODUCT TYPES

    /**
     * Product types represent the product of a sequence of types
     */
    public static class ProductType extends Type {
        /**
         * List of types in the product
         */
        private List<Type> types;

        /**
         * Constructor when list of types available
         */
        public ProductType(Location loc, List<Type> types) {
            super(loc, 0);
            this.types = types;
        }

        public ProductType(List<Type> types) {
            this(ErrorHandler.NO_LOCATION, types);
        }

        /**
         * Constructor allowing individual types to be specified
         */
        public ProductType(Location loc, Type... typeArray) {
            this(loc, Arrays.asList(typeArray));
        }

        /**
         * Constructor allowing individual types to be specified.
         * Predefined types do not have a location.
         */
        public ProductType(Type... typeArray) {
            this(ErrorHandler.NO_LOCATION, Arrays.asList(typeArray));
        }

        /**
         * The space required for a product of types is the sum of
         * the spaces required for each type in the product.
         */
        private int calcSpace(List<Type> types) {
            int space = 0;
            for (Type t : types) {
                space += t.getSpace();
            }
            return space;
        }

        public List<Type> getTypes() {
            return types;
        }

        /**
         * Resolve identifier references anywhere within type
         */
        @Override
        public ProductType resolveType() {
            if (!resolved) {
                /* Build a list of resolved types */
                List<Type> resolvedTypes = new LinkedList<>();
                for (Type t : types) {
                    resolvedTypes.add(t.resolveType());
                }
                types = resolvedTypes;
                space = calcSpace(types);
                resolved = true;
            }
            return this;
        }

        /**
         * Two product types are equal only if each element of the list
         * of types for one is equal to the corresponding element of the
         * list of types for the other.
         */
        @Override
        public boolean equals(Type other) {
            if (other instanceof ProductType) {
                List<Type> otherTypes = ((ProductType) other).getTypes();
                if (types.size() == otherTypes.size()) {
                    Iterator<Type> iterateOther = otherTypes.iterator();
                    for (Type t : types) {
                        Type otherType = iterateOther.next();
                        if (!t.equals(otherType)) {
                            return false;
                        }
                    }
                    /* If we reach here then every type in the product has
                     * matched the corresponding type in the other product
                     */
                    return true;
                }
            }
            /* other is not a ProductType or has a different number of types */
            return false;
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder("(");
            String sep = "";
            for (Type t : types) {
                result.append(sep).append(t.getName());
                sep = "*";
            }
            return result + ")";
        }
    }

    //******************** FUNCTION TYPES

    /**
     * Function types represent a function from an argument type
     * to a result type. This is used for the types of operators.
     */
    public static class FunctionType extends Type {
        /**
         * Type of the argument to the function
         */
        Type argType;
        /**
         * Type of the result of the function
         */
        Type resultType;

        /**
         * Basic constructor for Function types
         * @param loc location of the definition of the type
         * @param arg type of the argument to the function
         * @param result type of the result of the function
         */
        private FunctionType(Location loc, Type arg, Type result) {
            super(loc, 0);
            this.argType = arg;
            this.resultType = result;
        }
        /** Predefined types do not have a location */
        public FunctionType(Type arg, Type result) {
            this(ErrorHandler.NO_LOCATION, arg, result);
        }

        public Type getArgType() {
            return argType;
        }

        public Type getResultType() {
            return resultType;
        }

        /**
         * Resolve identifier references anywhere within type
         */
        @Override
        public FunctionType resolveType() {
            if (!resolved) {
                argType = argType.resolveType();
                resultType = resultType.resolveType();
                resolved = true;
            }
            return this;
        }

        /**
         * Two function types are equal only if their argument and result
         * types are equal.
         */
        @Override
        public boolean equals(Type other) {
            if (other instanceof FunctionType) {
                FunctionType otherFunction = (FunctionType) other;
                return getArgType().equals(otherFunction.getArgType()) &&
                        getResultType().equals(otherFunction.getResultType());
            }
            return false;
        }

        @Override
        public String toString() {
            return "(" + argType.getName() + "->" + resultType.getName() + ")";
        }
    }

    //******************** OPERATOR TYPES

    /**
     * Operators types represent a function from an argument type
     * to a result type. It includes the particular operation.
     */
    public static class OperatorType extends Type {
        /**
         * Function type of the operator
         */
        FunctionType opType;
        /**
         * Operator associated with type
         */
        Operator op;

        /**
         * Basic constructor for Function types
         * @param loc location of the definition of the type
         * @param opType function type of the operator
         */
        public OperatorType(Location loc, FunctionType opType, Operator op) {
            super(loc, 0);
            this.opType = opType;
            this.op = op;
        }
        /** Predefined types do not have a location */
        public OperatorType(FunctionType opType, Operator op) {
            this(ErrorHandler.NO_LOCATION, opType, op);
        }

        public FunctionType opType() {
            return opType;
        }

        public Operator getOperator() {
            return op;
        }

        @Override
        public String toString() {
            return opType.toString();
        }
    }

    //********************* INTERSECTION TYPES

    /**
     * Intersection types represent the intersection of a set of types.
     * They can be used as the types of overloaded operators.
     * For example "=" has two types to allow two integers to be compared
     * and two booleans to be compared.
     * The name intersection type is widely used but it is misleading in
     * this context because it is not set intersection. It is better
     * thought of as the greatest lower bound of types, i.e. the greatest
     * type that is a sub-type of all the types. If x is of the intersection
     * type, it is also of type T for each type T in the intersection type.
     */
    public static class IntersectionType extends Type {
        /**
         * Set of possible types. While a set would be a more appropriate
         * abstraction here, a list is used so that error messages that include
         * intersection types list the alternative types in the same order
         * independent of the compiler implementation used.
         */
        private List<Type> types;

        /**
         * @param typeArray - list of types in the intersection
         *                  requires the types in typeArray are distinct
         */
        IntersectionType(Location loc, Type... typeArray) {
            super(loc, 0);
            types = new ArrayList<>(typeArray.length);
            types.addAll(Arrays.asList(typeArray));
        }

        public List<Type> getTypes() {
            return types;
        }

        /**
         * Add a type to the list of types, but if it is a IntersectionType
         * flatten it and add each type in the intersection.
         */
        void addType(Type t) {
            if (t instanceof IntersectionType) {
                types.addAll(((IntersectionType) t).getTypes());
            } else {
                types.add(t);
            }
        }

        /**
         * Resolve identifier references anywhere within type
         */
        @Override
        public IntersectionType resolveType() {
            if (!resolved) {
                /* Build a list of resolved types */
                List<Type> resolvedTypes = new ArrayList<>(types.size());
                for (Type t : types) {
                    resolvedTypes.add(t.resolveType());
                }
                types = resolvedTypes;
                resolved = true;
            }
            return this;
        }

        /* Two intersection types are equal if they contain the same sets of
         * types.
         * @param other - type to be compared with 'this' type
         * @requires the lists in each intersection type have distinct elements
         */
        @Override
        public boolean equals(Type other) {
            if (other instanceof IntersectionType) {
                List<Type> otherTypes = ((IntersectionType) other).getTypes();
                if (types.size() == otherTypes.size()) {
                    for (Type t : types) {
                        if (!otherTypes.contains(t)) {
                            return false;
                        }
                    }
                    /* If we reach here then all types in 'this' intersection
                     * are also contained in the other intersection, and hence
                     * the two intersections are equivalent because they are
                     * the same size and both have no duplicates.
                     */
                    return true;
                }
            }
            /* other is not an IntersectionType or
             * has a different number of types */
            return false;
        }

        /**
         * An ExpNode can be coerced to a IntersectionType if it can be
         * coerced to one of the types of the intersection.
         *
         * @throws IncompatibleTypes exception if it is not possible to
         *                           coerce exp to any type within the intersection
         */
        @Override
        protected ExpNode coerce(ExpNode exp) throws IncompatibleTypes {
            /* We iterate through all the types in the intersection, trying
             * to coerce the exp to each, until one succeeds and we return
             * that coerced expression. If a coercion to a type in the
             * intersection fails it will throw an exception, which is caught.
             * Once caught we ignore the exception, and allow the for loop to
             * try the next type in the intersection.
             */
            errors.incDebug();
            for (Type toType : this.getTypes()) {
                try {
                    ExpNode newExp = toType.coerceToType(exp);
                    errors.debugMessage("Coerced " + exp + " to " +
                            toType.getName());
                    errors.decDebug();
                    return newExp;
                } catch (IncompatibleTypes ex) {
                    errors.debugMessage("cannot coerce " + exp + " to " +
                            toType.getName());
                    // allow "for" loop to try the next alternative
                }
            }
            errors.decDebug();
            /* If we get here, we were unable to to coerce exp to any one of
             * the types in the intersection, and hence we cannot coerce exp to
             * the intersection type.
             */
            throw new IncompatibleTypes("none of types match",
                    exp.getLocation());
        }

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder("(");
            String sep = "";
            for (Type t : types) {
                s.append(sep).append(t.getName());
                sep = " & ";
            }
            return s + ")";
        }
    }

    //********************* PROCEDURE TYPES

    public static class ProcedureType extends Type {
        public ProcedureType(Location loc) {
            /* Size of type allows for the procedure itself
             * to be a parameter. Ignore this unless implementing
             * procedures as parameters to procedures. */
            super(loc, 2 * SIZE_OF_ADDRESS);
        }

        public ProcedureType() {
            this(ErrorHandler.NO_LOCATION);
        }

        /**
         * Resolving a procedure's type involves resolving
         * the types of any parameters to the procedure.
         *
         * @return resolved type
         */
        @Override
        public ProcedureType resolveType() {
            resolved = true;
            return this;
        }

        @Override
        public String toString() {
            StringBuilder s = new StringBuilder("PROCEDURE(");
            s.append(")");
            return s.toString();
        }
    }


    //***************** TYPE IDENTIFIER TYPES
    /**
     * Status of resolution of reference.
     */
    private enum Status {Unresolved, Resolving, Resolved}

    /**
     * Type for a type identifier. Used until the type identifier can
     * be resolved. Note that for a type definition of the form
     *     type S = T;
     * the attribute name is "S" and the attribute id is "T".
     */
    public static class IdRefType extends Type {
        /**
         * Referenced identifier
         */
        private final String id;
        /**
         * Symbol table scope at the point of definition of the type
         * reference. Used when resolving the reference.
         */
        private final Scope scope;
        /**
         * Resolved real type, or ERROR_TYPE if cannot be resolved.
         */
        private Type realType;

        private Status status;

        public IdRefType(Location loc, String id, Scope scope) {
            super(loc, 0, false, id);
            this.id = id;
            this.scope = scope;
            this.status = Status.Unresolved;
        }

        /**
         * Resolve the type identifier and return the real type.
         */
        @Override
        public Type resolveType() {
            // System.out.println("Resolving type id " + id);
            switch (status) {
                case Unresolved:
                    status = Status.Resolving;
                    realType = ERROR_TYPE;
                    SymEntry entry = scope.lookupType(id);
                    if (entry != null) {
                        /* resolve identifiers in the referenced type */
                        entry.resolve();
                        /* if status of this entry has resolved then there was a
                         * circular reference and we leave the realType as
                         * ERROR_TYPE to avoid other parts of the compiler getting
                         * into infinite loops chasing types.
                         */
                        if (status == Status.Resolving) {
                            realType = entry.getType();
                        }
                        assert realType != null;
                    } else {
                        errors.error("undefined type: " + id, loc);
                    }
                    status = Status.Resolved;
                    break;
                case Resolving:
                    errors.error(id + " is circularly defined", loc);
                    /* Will resolve to ERROR_TYPE */
                    status = Status.Resolved;
                    break;
                case Resolved:
                    /* Already resolved */
                    break;
            }
            // System.out.println("Resolved type id " + name + " to " + realType);
            return realType;
        }

        public boolean defined() {
            return scope.lookupType(id) != null;
        }

        @Override
        public String toString() {
            if (resolved) {
                return realType.toString();
            } else {
                return "IdRef(" + id + ")";
            }
        }
    }

    //****************** ADDRESS TYPES

    /**
     * AddressType is the common part of ReferenceType (and PointerType)
     */
    public static class AddressType extends Type {
        /**
         * Type of addressed object
         */
        Type baseType;

        AddressType(Location loc, Type baseType) {
            super(loc, SIZE_OF_ADDRESS);
            this.baseType = baseType;
        }

        public Type getBaseType() {
            return baseType;
        }

        @Override
        public AddressType resolveType() {
            if (!resolved) {
                baseType = baseType.resolveType();
                resolved = true;
            }
            return this;
        }
    }

    //************************* REFERENCE TYPES

    /**
     * This method implements Rule Dereference in the static semantics if
     * applicable, otherwise it leaves the expression unchanged.
     * Optionally dereference a Reference type expression to get its base type
     * If exp is type ReferenceType(T) for some base type T,
     * a new DereferenceNode of type T is created with exp as a subtree
     * and returned, otherwise exp is returned unchanged.
     */
    public static ExpNode optDereferenceExp(ExpNode exp) {
        Type fromType = exp.getType();
        if (fromType instanceof ReferenceType) {
            errors.debugMessage("Coerce dereference " + fromType.getName());
            return new ExpNode.DereferenceNode(exp);
        } else {
            return exp;
        }
    }

    /**
     * If 'this' type is a reference type return its base type
     * otherwise just return 'this' type.
     * Default return 'this' - overridden in ReferenceType.
     */
    public Type optDereferenceType() {
        return this;
    }

    /**
     * Type used for variables in order to distinguish a variable
     * of type ref(int), say, from its value which is of type int.
     */
    public static class ReferenceType extends AddressType {

        public ReferenceType(Location loc, Type baseType) {
            super(loc, baseType);
        }

        public ReferenceType(Type baseType) {
            this(ErrorHandler.NO_LOCATION, baseType);
        }

        /**
         * If 'this' type is a reference type return its base type
         * otherwise just return 'this'.
         * As this subclass is ReferenceType, must return its base type.
         */
        @Override
        public Type optDereferenceType() {
            return baseType;
        }

        /**
         * Two reference types are equal only if their base types are equal
         */
        @Override
        public boolean equals(Type other) {
            return other instanceof ReferenceType &&
                    ((ReferenceType) other).getBaseType().equals(
                            this.getBaseType());
        }

        @Override
        public String toString() {
            return "ref(" + baseType.getName() + ")";
        }
    }

}
