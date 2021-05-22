package tree;

import java.util.LinkedList;
import java.util.List;

import syms.SymEntry;
import tree.StatementNode.BlockNode;

/**
 * class DeclNode - Handles Declarations lists and procedures.
 * DeclNode is an abstract class.
 * The classes defined within DeclNode extend it.
 */
public abstract class DeclNode {

    /**
     * Constructor
     */
    DeclNode() {
        super();
    }

    /**
     * Simple visitor pattern implemented in subclasses
     */
    public abstract void accept(DeclVisitor visitor);

    /**
     * Debugging output at level 0
     */
    @Override
    public String toString() {
        return toString(0);
    }

    /**
     * Debugging output of declarations
     */
    public abstract String toString(int level);

    /**
     * Tree node representing a list of (procedure) declarations
     */
    public static class DeclListNode extends DeclNode {
        final List<DeclNode> declarations;

        public DeclListNode() {
            declarations = new LinkedList<>();
        }

        List<DeclNode> getDeclarations() {
            return declarations;
        }

        public void addDeclaration(DeclNode declaration) {
            declarations.add(declaration);
        }

        @Override
        public void accept(DeclVisitor visitor) {
            visitor.visitDeclListNode(this);
        }

        public String toString(int level) {
            StringBuilder s = new StringBuilder();
            for (DeclNode decl : declarations) {
                s.append(StatementNode.newLine(level)).append(decl.toString(level));
            }
            return s.toString();
        }
    }

    /**
     * Tree node representing a single procedure.
     */
    public static class ProcedureNode extends DeclNode {
        final SymEntry.ProcedureEntry procEntry;
        final BlockNode block;

        public ProcedureNode(SymEntry.ProcedureEntry entry,
                             StatementNode.BlockNode block) {
            super();
            this.procEntry = entry;
            this.block = block;
        }

        @Override
        public void accept(DeclVisitor visitor) {
            visitor.visitProcedureNode(this);
        }

        public SymEntry.ProcedureEntry getProcEntry() {
            return procEntry;
        }

        public StatementNode.BlockNode getBlock() {
            return block;
        }

        public String toString(int level) {
            return "PROCEDURE " + procEntry.getIdent() +
                    " = " + block.toString(level + 1);
        }
    }

}
