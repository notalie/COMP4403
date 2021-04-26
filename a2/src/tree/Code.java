package tree;

import java.util.List;
import java.util.ArrayList;

import machine.StackMachine;
import machine.Instruction;
import machine.Operation;
import syms.SymEntry;
import syms.Type;

/**
 * class Code - store sequence of instructions
 */
public class Code {
    /**
     * List of instructions generated
     */
    private final List<Instruction> code;
    /**
     * Size of the instructions in words.
     * This is not just the length of the list.
     */
    private int size;
    /**
     * Code array is initially empty
     */
    public Code() {
        super();
        code = new ArrayList<>();
        size = 0;
    }

    public List<Instruction> getInstructionList() {
        return code;
    }

    public int size() {
        return size;
    }
    //---------------------------------------------------------------
    //--------------- Utility Code Generation Methods
    //---------------------------------------------------------------

    /**
     * Append the code sequence newCode to this code sequence.
     *
     * @param newCode sequence to be appended
     */
    public void append(Code newCode) {
        code.addAll(newCode.code);
        size += newCode.size();
    }

    /**
     * Generate instruction and append to code sequence.
     *
     * @param opcode of the generated instruction.
     */
    public void generateOp(Operation opcode) {
        code.add(new Instruction(opcode));
        size += opcode.getSize();
    }

    /**
     * Generate comment into code sequence.
     *
     * @param comment
     */
    public void genComment(String comment) {
        code.add(new Instruction.CommentInstruction(comment));
        // No change in size of code
    }

    /**
     * Generate a LoadConstant instruction and append to code sequence.
     *
     * @param value of the constant
     * @return location of the constant for later patching
     */
    public int genLoadConstant(int value) {
        int position = code.size();
        code.add(new Instruction.LoadConInstruction(value));
        size += Operation.LOAD_CON.getSize();
        return position;
    }

    /**
     * Update the LOAD_CON instruction at index position to load
     * the new value.
     */
    public void updateLoadCon(int position, int value) {
        Instruction instruction = code.get(position);
        if (instruction instanceof Instruction.LoadConInstruction) {
            ((Instruction.LoadConInstruction) instruction).setValue(value);
        } else {
            throw new Error("Code update of non-LOAD_CON instruction");
        }
    }

    /**
     * Generate a procedure call reference instruction
     */
    public void genProcCallRef(SymEntry.ProcedureEntry proc) {
        code.add(new Instruction.ProcRefInstruction(proc));
        size += Operation.LOAD_CON.getSize();
    }

    /**
     * Generate code to perform a logical negation.
     * False is represented by 0 and true by 1.
     * Using a bitwise not operation does not give the correct result.
     */
    public void genBoolNot() {
        generateOp(Operation.NEGATE);
        generateOp(Operation.ONE);
        generateOp(Operation.ADD);
    }

    /**
     * Generate code to load the address of a variable relative
     * to the current frame pointer. If the difference in level
     * is zero (a local variable) then just load offset, otherwise
     * generate the code to follow the static chain levelDiff number
     * of times and then make that address relative to the current
     * frame pointer and then add offset.
     *
     * @param levelDiff difference between the static level being
     *                  referenced and the current level.
     * @param offset    from frame pointer (at the appropriate level)
     */
    public void genMemRef(int levelDiff, int offset) {
        if (levelDiff == 0) {
            /* A local variable, so just load the offset from the frame pointer */
            genLoadConstant(offset);
        } else {
            /* Generate code to load the address of the frame containing the variable. */
            loadFrameAddress(levelDiff);
            /* Add the offset of the variable to get absolute address of variable. */
            genLoadConstant(offset);
            generateOp(Operation.ADD);
            /* Convert from absolute address to an address relative to the
             * current frame pointer.
             */
            generateOp(Operation.TO_LOCAL);
        }
    }

    /**
     * Generate the load instruction depending on size
     */
    public void genLoad(Type type) {
        if (type.getSpace() == 1) {
            /* A single word value is loaded with LOAD_FRAME */
            generateOp(Operation.LOAD_FRAME);
        } else {
            /* A multi-word value is loaded with LOAD_MULTI */
            genLoadConstant(type.getSpace());
            generateOp(Operation.LOAD_MULTI);
        }
    }

    /**
     * Generate a store instruction based on the size of values of the type
     */
    public void genStore(Type expType) {
        int size = expType.getSpace();
        if (size == 1) {
            /* For an expression that can fit in a single word,
             *  store that into the variable.
             */
            generateOp(Operation.STORE_FRAME);
        } else {
            /* For the assignment of one multi-word variable to another
             * generate a STORE_MULTI instruction to store the entire value.
             */
            genLoadConstant(size);
            generateOp(Operation.STORE_MULTI);
        }
    }

    /**
     * Generate the code for a procedure call including setting up
     * the static and dynamic links.
     *
     * @param levelDiff difference between the static level being
     *                  called and the current level.
     * @param proc      the procedure being called
     */
    public void genCall(int levelDiff, SymEntry.ProcedureEntry proc) {
        /* Set up the static link */
        loadFrameAddress(levelDiff);
        /* Call routine */
        genProcCallRef(proc);
        generateOp(Operation.CALL);
    }

    /**
     * Generate the code to chase the static link chain.
     *
     * @param levelDiff the number of frames to chase back.
     *                  requires 0 <= levelDiff
     */
    public void loadFrameAddress(int levelDiff) {
        assert 0 <= levelDiff;
        if (levelDiff == 0) {
            /* The static link is the current frame pointer */
            generateOp(Operation.ZERO);
            generateOp(Operation.TO_GLOBAL);
        } else {
            /* Load the static link of the current frame */
            generateOp(Operation.ZERO);
            generateOp(Operation.LOAD_FRAME);
            /* Follow the static link chain back levelDiff-1 times */
            for (int i = levelDiff - 1; i > 0; i--) {
                generateOp(Operation.LOAD_ABS);
            }
        }
    }

    /**
     * Size of instructions implementing jump_if_false and jump_if_true
     */
    static final int SIZE_JUMP_IF_FALSE =
            Operation.BR_FALSE.getSize() + Operation.LOAD_CON.getSize();
    static final int SIZE_JUMP_IF_TRUE = SIZE_JUMP_IF_FALSE;
    /**
     * Generate a JumpIf True to location offset. The branch address
     * is relative to the address of the instruction following the
     * BR_TRUE instruction.
     */
    public int genJumpIfTrue(int offset) {
        int position = genLoadConstant(offset);
        generateOp(Operation.BR_TRUE);
        return position;
    }

    /**
     * Generate a JumpIf False to location offset. The branch address
     * is relative to the address of the instruction following the
     * BR_FALSE instruction.
     */
    public int genJumpIfFalse(int offset) {
        int position = genLoadConstant(offset);
        generateOp(Operation.BR_FALSE);
        return position;
    }

    /**
     * Size of instructions implementing jump_always
     */
    static final int SIZE_JUMP_ALWAYS =
            Operation.BR.getSize() + Operation.LOAD_CON.getSize();

    /**
     * Generate an unconditional branch.
     */
    public int genJumpAlways(int offset) {
        int position = genLoadConstant(offset);
        generateOp(Operation.BR);
        return position;
    }

    /**
     * Generate a bounds check instruction.
     * Assumes the value to check is already on the stack.
     * If the bounds check succeeds the value checked is left
     * on the top of stack, otherwise the machine halts
     * with an OUT_OF_BOUNDS runtime error.
     */
    public void genBoundsCheck(int lower, int upper) {
        Code condCode = new Code();
        condCode.generateOp(Operation.DUP);
        condCode.genLoadConstant(lower);
        condCode.genLoadConstant(upper);
        condCode.generateOp(Operation.BOUND);
        Code stopCode = new Code();
        stopCode.genLoadConstant(StackMachine.OUT_OF_BOUNDS);
        stopCode.generateOp(Operation.STOP);
        genIfThenElse(condCode, new Code(), stopCode);
    }

    /**
     * Generate code to allocate stack space
     */
    public void genAllocStack(int variableSpace) {
        if (variableSpace != 0) {
            genLoadConstant(variableSpace);
            generateOp(Operation.ALLOC_STACK);
        }
    }

    /**
     * Generate code to deallocate stack space
     */
    public void genDeallocStack(int variableSpace) {
        if (variableSpace != 0) {
            genLoadConstant(variableSpace);
            generateOp(Operation.DEALLOC_STACK);
        }
    }

    /**
     * Generate code for an if-then-else.
     * Optimises for the cases when either to then or else parts are empty.
     */
    public void genIfThenElse(Code condition, Code thenCode, Code elseCode) {
        /* Note that the condition code will be modified (extended) */
        append(condition);
        if (thenCode.size() == 0) {
            // Empty then part uses jump if true
            genJumpIfTrue(elseCode.size());
            genComment("else part:");
            append(elseCode);
        } else if (elseCode.size() == 0) {
            // empty else part uses branch iff false
            genJumpIfFalse(thenCode.size());
            genComment("then part:");
            append(thenCode);
        } else {
            // both parts non-empty
            genJumpIfFalse(thenCode.size() + Code.SIZE_JUMP_ALWAYS);
            genComment("then part:");
            append(thenCode);
            genJumpAlways(elseCode.size());
            genComment("else part:");
            append(elseCode);
        }
    }

    @Override
    public String toString() {
        StringBuilder instructions = new StringBuilder("[");
        String sep = "";
        for (Instruction inst : code) {
            instructions.append(sep).append(inst);
            sep = ",";
        }
        return instructions + "]";
    }
}
