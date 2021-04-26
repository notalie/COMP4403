package tree;

/**
 * interface StatementVisitor - Provides the interface for the visitor pattern
 * to be applied to an abstract syntax tree node for a statement.
 * A class implementing this interface must provide implementations for visit
 * methods for each of the statement node type.
 */
public interface StatementVisitor {

    void visitBlockNode(StatementNode.BlockNode node);

    void visitStatementErrorNode(StatementNode.ErrorNode node);

    void visitStatementListNode(StatementNode.ListNode node);

    void visitAssignmentNode(StatementNode.AssignmentNode node);

    void visitReadNode(StatementNode.ReadNode node);

    void visitWriteNode(StatementNode.WriteNode node);

    void visitCallNode(StatementNode.CallNode node);

    void visitIfNode(StatementNode.IfNode node);

    void visitWhileNode(StatementNode.WhileNode node);

}
