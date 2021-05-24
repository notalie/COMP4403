package tree;

import java.util.*;

import source.VisitorDebugger;
import source.Errors;
import java_cup.runtime.ComplexSymbolFactory.Location;
import sun.jvm.hotspot.debugger.cdbg.Sym;
import syms.Predefined;
import syms.SymEntry;
import syms.Scope;
import syms.Type;
import syms.Type.IncompatibleTypes;
import tree.DeclNode.DeclListNode;
import tree.StatementNode.*;

/**
 * class StaticSemantics - Performs the static semantic checks on
 * the abstract syntax tree using a visitor pattern to traverse the tree.
 * See the notes on the static semantics of PL0 to understand the PL0
 * type system in detail.
 */
public class StaticChecker implements DeclVisitor, StatementVisitor,
        ExpTransform<ExpNode> {

    /**
     * The static checker maintains a reference to the current
     * symbol table scope for the procedure currently being processed.
     */
    private Scope currentScope;
    /**
     * Errors are reported through the error handler.
     */
    private final Errors errors;
    /**
     * Debug messages are reported through the visitor debugger.
     */
    private final VisitorDebugger debug;

    /**
     * Construct a static checker for PL0.
     *
     * @param errors is the error message handler.
     */
    public StaticChecker(Errors errors) {
        super();
        this.errors = errors;
        debug = new VisitorDebugger("checking", errors);
    }

    /**
     * The tree traversal starts with a call to visitProgramNode.
     * Then its descendants are visited using visit methods for each
     * node type, which are called using the visitor pattern "accept"
     * method or "transform" for expression nodes of the abstract
     * syntax tree node.
     */
    public void visitProgramNode(DeclNode.ProcedureNode node) {
        beginCheck("Program");
        // The main program is a special case of a procedure
        visitProcedureNode(node);
        endCheck("Program");
    }

    /**
     * Procedure, function or main program node
     */
    public void visitProcedureNode(DeclNode.ProcedureNode node) {
        beginCheck("Procedure");
        SymEntry.ProcedureEntry procEntry = node.getProcEntry();
        Type.ProcedureType procType = procEntry.getType();
        // Set the current symbol table scope to that for the procedure.
        Scope localScope = procEntry.getLocalScope();

        for (SymEntry.ParamEntry paramEntry : procType.getFormalParams()) {
            if (paramEntry.getDefaultExp() != null) { // Default expression exists
                // Need to coerce the default expression because I'm not sure if it's done in the transform method
                ExpNode defaultParam = paramEntry.getDefaultExp().transform(this);
                defaultParam.setType(defaultParam.getType().resolveType());

                Type paramType = paramEntry.getType().optDereferenceType();
                Type defaultType = defaultParam.getType().optDereferenceType();

                if (paramEntry.isRef() && paramType != defaultType) {
                    // Check that the reference types refer to the same dereference type
                    staticError("default expression must be of type " + paramEntry.getType(),
                            defaultParam.getLocation());
                } else if (paramType != defaultType) {
                    // Varying static errors depending on if ref or not
                    staticError("cannot coerce " + defaultType + " to " + paramType, defaultParam.getLocation());
                }

                paramEntry.setDefaultParam(defaultParam);
            }
            localScope.addEntry(paramEntry); // add entry to the local scope
        }

        // Save the block's abstract syntax tree in the procedure entry
        procEntry.setBlock(node.getBlock());
        /* Resolve all references to identifiers within the declarations. */
        localScope.resolveScope();
        // Enter the local scope of the procedure
        currentScope = localScope;
        // Check the block of the procedure.
        visitBlockNode(node.getBlock());
        // Restore the symbol table to the parent scope
        currentScope = currentScope.getParent();
        endCheck("Procedure");
    }

    /**
     * Block node
     */
    public void visitBlockNode(BlockNode node) {
        beginCheck("Block");
        node.getProcedures().accept(this);  // Check the procedures, if any
        node.getBody().accept(this);        // Check the body of the block
        endCheck("Block");
    }

    /**
     * Process the list of procedure declarations
     */
    public void visitDeclListNode(DeclListNode node) {
        beginCheck("DeclList");
        for (DeclNode declaration : node.getDeclarations()) {
            declaration.accept(this);
        }
        endCheck("DeclList");
    }


    /*************************************************
     *  Statement node static checker visit methods
     *************************************************/
    public void visitStatementErrorNode(StatementNode.ErrorNode node) {
        beginCheck("StatementError");
        // Nothing to check - already invalid.
        endCheck("StatementError");
    }

    /**
     * Assignment statement node
     */
    public void visitAssignmentNode(StatementNode.AssignmentNode node) {
        beginCheck("Assignment");
        // Check the left side left value.
        ExpNode left = node.getVariable().transform(this);
        node.setVariable(left);
        // Check the right side expression.
        ExpNode exp = node.getExp().transform(this);
        node.setExp(exp);
        // Validate that it is a true left value and not a constant
        if (left.getType() instanceof Type.ReferenceType) {
            /* Validate that the right side expression is assignment
             * compatible with the left value. This requires that the
             * right side expression is coerced to the base type of
             * type of the left side LValue. */
            Type baseType = ((Type.ReferenceType) left.getType()).getBaseType();
            node.setExp(baseType.coerceExp(exp));
        } else if (left.getType() != Type.ERROR_TYPE) {
                staticError("variable expected", left.getLocation());
        }
        endCheck("Assignment");
    }

    /**
     * Reads an integer value from input
     */
    public void visitReadNode(StatementNode.ReadNode node) {
        beginCheck("Read");
        // Check the left value
        ExpNode lValue = node.getLValue().transform(this);
        node.setLValue(lValue);
        // Validate that it is a true left value of type ref(int)
        if (lValue.getType() instanceof Type.ReferenceType) {
            Type.ReferenceType refType = (Type.ReferenceType)lValue.getType();
            if (!Predefined.INTEGER_TYPE.equals(refType.getBaseType())) {
                staticError("integer variable expected", lValue.getLocation());
            }
        } else {
            staticError("variable expected", lValue.getLocation());
        }
        endCheck("Read");
    }

    /**
     * Write statement node
     */
    public void visitWriteNode(StatementNode.WriteNode node) {
        beginCheck("Write");
        // Check the expression being written.
        ExpNode exp = node.getExp().transform(this);
        // coerce expression to be of type integer,
        // or complain if not possible.
        node.setExp(Predefined.INTEGER_TYPE.coerceExp(exp));
        endCheck("Write");
    }


    /**
     * Call statement node
     */
    public void visitCallNode(StatementNode.CallNode node) {
        beginCheck("Call");
        SymEntry.ProcedureEntry procEntry = null;
        // Look up the symbol table entry for the procedure.
        SymEntry entry = currentScope.lookup(node.getId());
        if (entry instanceof SymEntry.ProcedureEntry) {
            procEntry = (SymEntry.ProcedureEntry) entry;
            node.setEntry(procEntry);
        } else {
            staticError("Procedure identifier required", node.getLocation());
            endCheck("Call");
            return;
        }
        // Get the type of procedure from the sym entry
        Type.ProcedureType procedureType = procEntry.getType();

        // What should be in the type that was specified
        List<SymEntry.ParamEntry> formalParams = procedureType.getFormalParams();

        // No need to parse anything if there's no former parameters in the procedure being called
        if (formalParams.size() == 0) {
            // Check that no actual parameters are being passed in
            if (node.getParams().size() != 0 && formalParams.size() == 0) {
                for (ExpNode param: node.getParams()) {
                    staticError("not a parameter of procedure", param.getLocation());
                }
            }
            endCheck("Call");
            return;
        }

        // Formal params to change for the procedure before passing in
        HashMap<String, ExpNode.ActualParamNode> newFormalParams = new HashMap<>();

        // Check each actual parameter
        for (ExpNode p: node.getParams()) {
            boolean found = false;

            // Ignore Error nodes
            if (p instanceof ExpNode.ErrorNode) {
                continue;
            }

            // Change the values in the parameters
            p.transform(this);
            ExpNode.ActualParamNode param = (ExpNode.ActualParamNode)p;
            // For checking if all defaults are covered + setting the transformed default expressions in the node
            newFormalParams.put(param.getId(), param);

            for (SymEntry se : formalParams) {
                SymEntry.ParamEntry paramEntry = (SymEntry.ParamEntry)se;
                // Found a matching identifier within the scope
                if (paramEntry.getIdent().equals(param.getId())) {
                    found = true; // Found the default parameter

                    // get two dereference types from given
                    Type formalParamType = se.getType().optDereferenceType();
                    Type paramType = param.getCondition().getType().optDereferenceType();

                    try {
                        // Check if the condition can be coerced to the formalParamType
                        formalParamType.coerceToType(param.getCondition());
                    } catch (Type.IncompatibleTypes e) { // couldn't coerce
                        if (paramEntry.isRef()) { // Differing static error messages,
                            // wasn't sure how to get the ref( part of the string showing so I did it manually sorry ;/
                            staticError("actual parameter type should be " + se.getType() +
                                    " not ref(" + paramType + ")", param.getCondition().getLocation());
                        } else {
                            staticError("cannot coerce " + paramType + " to " +
                                    formalParamType, param.getCondition().getLocation());
                        }
                        continue; // To ignore the error checking below
                    }
                    // If actual parameter is a ref and ref types aren't the same,
                    // but coercion was successful, give a static error
                    if (paramEntry.isRef() && paramType != formalParamType) {
                        staticError("actual parameter type should be " + se.getType() +
                                " not ref(" + paramType.getName() + ")", param.getCondition().getLocation());
                    }
                }
            }
            // Couldn't find the actual parameter in the given former parameters list/doesn't exist
            if (!found) {
                staticError("not a parameter of procedure", param.getLocation());
            }
        }
        // Set the params as the new transformed values
        List<ExpNode> newList = new ArrayList<>();

        // Need to make sure that every former parameter has been covered in the previous for loop
        // give static error if there is an id that was not transformed that doesn't have a default
        for (SymEntry.ParamEntry paramEntry : formalParams) {
            if (newFormalParams.containsKey(paramEntry.getIdent())) {
                newList.add(newFormalParams.get(paramEntry.getIdent()));
            } else if (paramEntry.getDefaultExp() != null) {
                newList.add(new ExpNode.ActualParamNode(paramEntry.getLocation(), paramEntry.getIdent(),
                        paramEntry.getDefaultExp().transform(this)));
            } else {
                staticError("no actual parameter for " + paramEntry.getIdent(), node.getLocation());
            }
        }
        node.setParams(newList);
        endCheck("Call");
    }

    /**
     * Statement list node
     */
    public void visitStatementListNode(StatementNode.ListNode node) {
        beginCheck("StatementList");
        for (StatementNode s : node.getStatements()) {
            s.accept(this);
        }
        endCheck("StatementList");
    }

    /**
     * Check that the expression node can be coerced to boolean
     */
    private ExpNode checkCondition(ExpNode cond) {
        // Check and transform the condition
        cond = cond.transform(this);
        /* Validate that the condition is boolean, which may require
         * coercing the condition to be of type boolean. */
        return Predefined.BOOLEAN_TYPE.coerceExp(cond);
    }

    /**
     * If statement node
     */
    public void visitIfNode(StatementNode.IfNode node) {
        beginCheck("If");
        // Check the condition and replace with (possibly) transformed node
        node.setCondition(checkCondition(node.getCondition()));
        node.getThenStmt().accept(this);  // Check the 'then' part
        node.getElseStmt().accept(this);  // Check the 'else' part.
        endCheck("If");
    }

    /**
     * While statement node
     */
    public void visitWhileNode(StatementNode.WhileNode node) {
        beginCheck("While");
        // Check the condition and replace with (possibly) transformed node
        node.setCondition(checkCondition(node.getCondition()));
        node.getLoopStmt().accept(this);  // Check the body of the loop
        endCheck("While");
    }

    /*************************************************
     *  Expression node static checker visit methods.
     *  The static checking visitor methods for expressions
     *  transform the expression to include resolved identifier
     *  nodes, and add nodes like dereference nodes, and
     *  narrow and widen subrange nodes.
     *  These ensure that the transformed tree is type consistent
     *  and must ensure the type of the node is set appropriately.
     *************************************************/
    public ExpNode visitErrorExpNode(ExpNode.ErrorNode node) {
        beginCheck("ErrorExp");
        // Nothing to do - already invalid.
        endCheck("ErrorExp");
        return node;
    }

    /**
     * Constant expression node
     */
    public ExpNode visitConstNode(ExpNode.ConstNode node) {
        beginCheck("Const");
        // type already set up
        endCheck("Const");
        return node;
    }

    /**
     * Handles binary operators - Operators can be overloaded
     */
    public ExpNode visitBinaryNode(ExpNode.BinaryNode node) {
        beginCheck("Binary");
        /* Check the arguments to the operator */
        ExpNode left = node.getLeft().transform(this);
        ExpNode right = node.getRight().transform(this);
        /* Lookup the operator in the symbol table to get its type.
         * The operator may not be defined.
         */
        SymEntry.OperatorEntry opEntry = currentScope.lookupOperator(node.getOp().getName());
        if (opEntry == null) {
            staticError("operator not defined", node.getLocation());
            node.setType(Type.ERROR_TYPE);
            node.setOp(Operator.INVALID_OP);
        } else if (opEntry.getType() instanceof Type.OperatorType) {
            /* The operator is not overloaded. Its type is represented
             * by a FunctionType from its argument's type to its
             * result type.
             */
            Type.FunctionType fType = ((Type.OperatorType)opEntry.getType()).opType();
            List<Type> argTypes = ((Type.ProductType)fType.getArgType()).getTypes();
            node.setLeft(argTypes.get(0).coerceExp(left));
            node.setRight(argTypes.get(1).coerceExp(right));
            node.setType(fType.getResultType());
            node.setOp(((Type.OperatorType)opEntry.getType()).getOperator());
        } else if (opEntry.getType() instanceof Type.IntersectionType) {
            /* The operator is overloaded. Its type is represented
             * by an IntersectionType containing a set of possible
             * types for the operator, each of which is a FunctionType.
             * Each possible type is tried until one succeeds.
             */
            debugMessage("Coercing " + left + " and " + right + " to " + opEntry.getType());
            errors.incDebug();
            for (Type t : ((Type.IntersectionType) opEntry.getType()).getTypes()) {
                Type.FunctionType fType = ((Type.OperatorType)t).opType();
                List<Type> argTypes = ((Type.ProductType)fType.getArgType()).getTypes();
                try {
                    /* Coerce the argument to the argument type for
                     * this operator type. If the coercion fails an
                     * exception will be trapped and an alternative
                     * function type within the intersection tried.
                     */
                    ExpNode newLeft = argTypes.get(0).coerceToType(left);
                    ExpNode newRight = argTypes.get(1).coerceToType(right);
                    /* Both coercions succeeded if we get here else exception was thrown */
                    node.setLeft(newLeft);
                    node.setRight(newRight);
                    node.setType(fType.getResultType());
                    node.setOp(((Type.OperatorType)t).getOperator());
                    errors.decDebug();
                    endCheck("Binary");
                    return node;
                } catch (IncompatibleTypes ex) {
                    // Allow "for" loop to try an alternative
                }
            }
            errors.decDebug();
            debugMessage("Failed to coerce " + left + " and " + right +
                    " to " + opEntry.getType());
            // no match in intersection type
            staticError("Type of argument (" + left.getType().getName() + "*" +
                    right.getType().getName() +
                    ") does not match " + opEntry.getType().getName(), node.getLocation());
            node.setType(Type.ERROR_TYPE);
        } else {
            errors.fatal("Invalid operator type", node.getLocation());
        }
        endCheck("Binary");
        return node;
    }

    /**
     * Handles unary operators - Operators can be overloaded
     */
    public ExpNode visitUnaryNode(ExpNode.UnaryNode node) {
        beginCheck("Unary");
        /* Check the argument to the operator */
        ExpNode arg = node.getArg().transform(this);
        /* Lookup the operator in the symbol table to get its type.
         * The operator may not be defined.
         */
        SymEntry.OperatorEntry opEntry = currentScope.lookupOperator(node.getOp().getName());
        if (opEntry == null) {
            staticError("operator not defined", node.getLocation());
            node.setType(Type.ERROR_TYPE);
            node.setOp(Operator.INVALID_OP);
        } else if (opEntry.getType() instanceof Type.OperatorType) {
            /* The operator is not overloaded. Its type is represented
             * by a FunctionType from its argument's type to its
             * result type.
             */
            Type.FunctionType fType = ((Type.OperatorType)opEntry.getType()).opType();
            Type argType = fType.getArgType();
            node.setArg(argType.coerceExp(arg));
            node.setType(fType.getResultType());
            node.setOp(((Type.OperatorType)opEntry.getType()).getOperator());
        } else if (opEntry.getType() instanceof Type.IntersectionType) {
            /* The operator is overloaded. Its type is represented
             * by an IntersectionType containing a set of possible
             * types for the operator, each of which is a FunctionType.
             * Each possible type is tried until one succeeds.
             */
            debugMessage("Coercing " + arg + " to " + opEntry.getType());
            errors.incDebug();
            for (Type t : ((Type.IntersectionType) opEntry.getType()).getTypes()) {
                Type.FunctionType fType = ((Type.OperatorType)t).opType();
                Type argType = fType.getArgType();
                try {
                    /* Coerce the argument to the argument type for
                     * this operator type. If the coercion fails an
                     * exception will be trapped and an alternative
                     * function type within the intersection tried.
                     */
                    ExpNode newArg = argType.coerceToType(arg);
                    /* The coercion succeeded if we get here */
                    node.setArg(newArg);
                    node.setType(fType.getResultType());
                    node.setOp(((Type.OperatorType)t).getOperator());
                    errors.decDebug();
                    endCheck("Unary");
                    return node;
                } catch (IncompatibleTypes ex) {
                    // Allow "for" loop to try an alternative
                }
            }
            errors.decDebug();
            debugMessage("Failed to coerce " + arg + " to " + opEntry.getType());
            // no match in intersection type
            staticError("Type of argument " + arg.getType().getName() +
                    " does not match " + opEntry.getType().getName(), node.getLocation());
            node.setType(Type.ERROR_TYPE);
        } else {
            errors.fatal("Invalid operator type", node.getLocation());
        }
        endCheck("Unary");
        return node;
    }


    /**
     * A DereferenceNode allows a variable (of type ref(int) say) to be
     * dereferenced to get its value (of type int).
     */
    public ExpNode visitDereferenceNode(ExpNode.DereferenceNode node) {
        beginCheck("Dereference");
        // Check the left value referred to by this dereference node
        ExpNode lVal = node.getLeftValue().transform(this);
        node.setLeftValue(lVal);
        /* The type of the dereference node is the base type of its
         * left value. */
        Type lValueType = lVal.getType();
        if (lValueType instanceof Type.ReferenceType) {
            node.setType(lValueType.optDereferenceType()); // not optional here
        } else if (lValueType != Type.ERROR_TYPE) { // avoid cascading errors
            staticError("cannot dereference an expression which is not a reference",
                    node.getLocation());
        }
        endCheck("Dereference");
        return node;
    }

    /**
     * When parsing an identifier within an expression one cannot tell
     * whether it has been declared as a constant or an identifier.
     * Here we check which it is and return either a constant or
     * a variable node.
     */
    public ExpNode visitIdentifierNode(ExpNode.IdentifierNode node) {
        beginCheck("Identifier");
        // First we look up the identifier in the symbol table.
        ExpNode newNode;
        SymEntry entry = currentScope.lookup(node.getId());
        if (entry instanceof SymEntry.ConstantEntry) {
            // Set up a new node which is a constant.
            debugMessage("Transformed " + node.getId() + " to Constant");
            SymEntry.ConstantEntry constEntry =
                    (SymEntry.ConstantEntry) entry;
            newNode = new ExpNode.ConstNode(node.getLocation(),
                    constEntry.getType(), constEntry.getValue());
        } else if (entry instanceof SymEntry.VarEntry) {
            debugMessage("Transformed " + node.getId() + " to Variable");
            // Set up a new node which is a variable.
            SymEntry.VarEntry varEntry = (SymEntry.VarEntry) entry;
            newNode = new ExpNode.VariableNode(node.getLocation(), varEntry);
        } else {
            // Undefined identifier or a type or procedure identifier.
            // Set up new node to be an error node.
            newNode = new ExpNode.ErrorNode(node.getLocation());
            //System.out.println("Entry = " + entry);
            staticError("Constant or variable identifier required", node.getLocation());
        }
        endCheck("Identifier");
        return newNode;
    }

    /**
     * Variable node is set up by Identifier node - no checks needed
     */
    public ExpNode visitVariableNode(ExpNode.VariableNode node) {
        beginCheck("Variable");
        // Type already set up
        endCheck("Variable");
        return node;
    }

    /**
     * Narrow subrange node constructed during coerce - no checking needed
     */
    public ExpNode visitNarrowSubrangeNode(ExpNode.NarrowSubrangeNode node) {
        beginCheck("NarrowSubrange");
        // Nothing to do.
        endCheck("NarrowSubrange");
        return node;
    }

    /**
     * Widen subrange node constructed during coerce - no checking needed
     */
    public ExpNode visitWidenSubrangeNode(ExpNode.WidenSubrangeNode node) {
        beginCheck("WidenSubrange");
        // Nothing to do.
        endCheck("WidenSubrange");
        return node;
    }

    /**
     * ActualParamNode checking, need to transform the condition associated with it too
     */
    public ExpNode visitActualParamNode(ExpNode.ActualParamNode node) {
        beginCheck("ActualParamNode");
        node.setCondition(node.getCondition().transform(this));
        endCheck("ActualParamNode");
        return node;
    }

    //**************************** Support Methods

    /**
     * Push current node onto debug rule stack and increase debug level
     */
    private void beginCheck(String nodeName) {
        debug.beginDebug(nodeName);
    }

    /**
     * Pop current node from debug rule stack and decrease debug level
     */
    private void endCheck(String nodeName) {
        debug.endDebug(nodeName);
    }

    /**
     * Debugging message output
     */
    private void debugMessage(String msg) {
        errors.debugMessage(msg);
    }

    /**
     * Error message handle for parsing errors
     */
    private void staticError(String msg, Location loc) {
        errors.debugMessage(msg);
        errors.error(msg, loc);
    }
}
