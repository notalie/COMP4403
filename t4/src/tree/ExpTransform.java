package tree;

/**
 * interface ExpTransform - Handles visitor pattern for transforming expressions.
 */
public interface ExpTransform<ResultType> {
    ResultType visitErrorExpNode(ExpNode.ErrorNode node);

    ResultType visitConstNode(ExpNode.ConstNode node);

    ResultType visitIdentifierNode(ExpNode.IdentifierNode node);

    ResultType visitVariableNode(ExpNode.VariableNode node);

    ResultType visitBinaryNode(ExpNode.BinaryNode node);

    ResultType visitUnaryNode(ExpNode.UnaryNode node);

    ResultType visitDereferenceNode(ExpNode.DereferenceNode node);

    ResultType visitNarrowSubrangeNode(ExpNode.NarrowSubrangeNode node);

    ResultType visitWidenSubrangeNode(ExpNode.WidenSubrangeNode node);

}
