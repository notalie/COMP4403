package syms;

import source.ErrorHandler;
import java_cup.runtime.ComplexSymbolFactory.Location;
import tree.ConstExp;
import tree.ExpNode;
import syms.Type.IncompatibleTypes;
import junit.framework.TestCase;

/**
 * class TestType - JUnit test for class Type.
 */
public class TypeTest extends TestCase {

    public TypeTest(String arg0) {
        super(arg0);
    }

    private Type et;
    private Type.ScalarType it;
    private Type.ScalarType bt;
    private Type.ProcedureType pt;
    private Type.SubrangeType ist;
    private Type.SubrangeType bst;
    private Type.ReferenceType rit;
    private Type.ProductType iit;
    private Type.ProductType bbt;
    private Type.FunctionType iiit;
    private Type.FunctionType iibt;
    private Type.FunctionType bbbt;
    private ExpNode.ConstNode ix;
    private ExpNode.VariableNode ivx;
    private ExpNode.NarrowSubrangeNode isx;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Location noLoc = ErrorHandler.NO_LOCATION;
        ErrorHandler.getErrorHandler();
        SymbolTable symbolTable = new SymbolTable();
        Scope currentScope = symbolTable.getPredefinedScope();
        et = Type.ERROR_TYPE;
        it = Predefined.INTEGER_TYPE;
        bt = Predefined.BOOLEAN_TYPE;
        pt = new Type.ProcedureType().resolveType();
        ist = new Type.SubrangeType(
                new ConstExp.NumberNode(noLoc, it, 3),
                new ConstExp.NumberNode(noLoc, it, 7));
        ist.resolveType();
        bst = new Type.SubrangeType(
                new ConstExp.NumberNode(noLoc, bt, 0),
                new ConstExp.NumberNode(noLoc, bt, 1));
        bst.resolveType();
        Type.SubrangeType isst = new Type.SubrangeType(
                new ConstExp.NumberNode(noLoc, ist, 5),
                new ConstExp.NumberNode(noLoc, ist, 7));
        isst.resolveType();

        rit = new Type.ReferenceType(it);
        iit = new Type.ProductType(it, it);
        iit.resolveType();
        bbt = new Type.ProductType(bt, bt);
        bbt.resolveType();
        iiit = new Type.FunctionType(iit, it);
        iibt = new Type.FunctionType(iit, bt);
        bbbt = new Type.FunctionType(bbt, bt);

        ix = new ExpNode.ConstNode(null, it, 42);
        ix.setType(it);
        SymEntry.VarEntry iv = new SymEntry.VarEntry("iv", null, it);
        iv.setScope(currentScope);
        ivx = new ExpNode.VariableNode(null, iv);
        // ivx.setType(it);
        // ExpNode.DereferenceNode rix = new ExpNode.DereferenceNode(ivx);
        isx = new ExpNode.NarrowSubrangeNode(ist, ix);
    }

    /*
     * Test method for 'pl0.symbol_table.Type.getSpace()'
     */
    public void testGetSpace() {
        assertEquals(0, et.getSpace());
        assertEquals(1, it.getSpace());
        assertEquals(1, bt.getSpace());
        assertEquals(2, pt.getSpace());
        assertEquals(1, ist.getSpace());
        assertEquals(3, ist.getLower());
        assertEquals(7, ist.getUpper());
        assertEquals(1, bst.getSpace());
        assertEquals(0, bst.getLower());
        assertEquals(1, bst.getUpper());
        assertEquals(1, rit.getSpace());
        assertEquals(2, iit.getSpace());
        assertEquals(2, bbt.getSpace());
    }

    /*
     * Test method for 'pl0.symbol_table.Type.coerce()'
     */
    public void testCoerce() throws IncompatibleTypes {
        ExpNode result = it.coerceToType(ix);
        assertSame("int compatible with int", result, ix);
        result = it.coerceToType(ivx);
        assertTrue("int variable coerces to dereference",
                result instanceof ExpNode.DereferenceNode &&
                        ((ExpNode.DereferenceNode) result).getLeftValue() == ivx);
        result = ist.coerceToType(ix);
        assertTrue("int coerces to subrange of int",
                result instanceof ExpNode.NarrowSubrangeNode &&
                        ((ExpNode.NarrowSubrangeNode) result).getExp() == ix);
        result = it.coerceToType(isx);
        assertTrue("int subrange coerces to int" + result,
                result instanceof ExpNode.WidenSubrangeNode &&
                        ((ExpNode.WidenSubrangeNode) result).getExp() == isx);
    }

    /*
     * Test method for 'pl0.symbol_table.Type.toString()'
     */
    public void testToString() {
        assertEquals("int", it.toString());
        assertEquals("boolean", bt.toString());
        assertEquals("int[3..7]", ist.toString());
        assertEquals("boolean[0..1]", bst.toString());
        assertEquals("ref(int)", rit.toString());
        assertEquals("(int*int)", iit.toString());
        assertEquals("(boolean*boolean)", bbt.toString());
        assertEquals("((int*int)->int)", iiit.toString());
        assertEquals("((int*int)->boolean)", iibt.toString());
        assertEquals("((boolean*boolean)->boolean)", bbbt.toString());
    }
}
