package machine;


import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.EnumSet;
import java.io.PrintStream;

import source.ErrorHandler;
import source.Errors;
import syms.Predefined;
import syms.SymEntry;
import syms.Type;
import tree.Procedures;
import tree.Procedures.ProcedureCode;

/**
 * class StackMachine - Implementation of an emulation engine and code writer
 * for the Stack Machine.
 */

public class StackMachine {

    /**
     * Start of code within memory
     */
    public final static int CODE_START = 1000;
    /**
     * Size of memory
     */
    private final static int MEM_LIMIT = 10000;
    /**
     * Address way outside memory
     */
    public final static int NULL_ADDR = 0x80808080;

    /**
     * Memory array - stack and heap and code
     */
    private int[] memory = new int[MEM_LIMIT];
    /**
     * Location to store the next instruction during code generation
     */
    private int currLocn = CODE_START;
    /**
     * Stack machine running?
     */
    private boolean running = false;

    /**
     * Stack machine stop codes
     */
    public static final int OUT_OF_BOUNDS = 1;

    /**
     * Tracing constants (unioned together)
     */
    public enum Trace {
        MEM,
        CALLS,
        JUMPS,
        STACK,
        STATE
    }

    /**
     * Trace everything
     */
    public static EnumSet<Trace> TRACE_ALL = EnumSet.allOf(Trace.class);
    /**
     * No tracing at all.
     */
    public static EnumSet<Trace> TRACE_NONE =
            EnumSet.complementOf(TRACE_ALL);
    /**
     * Current tracing during execution of stack machine
     */
    private EnumSet<Trace> tracing = TRACE_NONE;

    /**
     * Output stream
     */
    private PrintStream outStream;
    /**
     * Object to handle error reports
     */
    private Errors errors;
    /**
     * Stores addresses of procedure starts
     */
    private Procedures procedures;

    /**
     * Bottom of stack
     */
    private final int STACK_START = 0;
    /**
     * Program counter
     */
    private int pc;
    /**
     * Frame pointer
     */
    private int fp = STACK_START;
    /**
     * Top of stack pointer - always one past top
     */
    private int sp = STACK_START;
    /**
     * Top of stack limit = bottom of heap limit
     */
    private int limit = CODE_START;
    /**
     * Standard input line reader
     */
    private BufferedReader in =
            new BufferedReader(new InputStreamReader(System.in));

    /****************************** Constructors **************************/

    public StackMachine(Errors errors, PrintStream outStream,
                        boolean listing, Procedures procedures) {
        this.errors = errors;
        this.outStream = outStream;
        this.procedures = procedures;
        for (int i = 0; i < MEM_LIMIT; i++) {
            memory[i] = NULL_ADDR;  // out of memory address
        }
        for (ProcedureCode proc : procedures.getProcedureEntries()) {
            if (listing) {
                outStream.println("Procedure " +
                        proc.getLocals().getOwnerEntry().getIdent());
            }
            if (proc.getName().equals("<main>")) {
                /* Set the start location for execution */
                pc = currLocn;
            }
            for (Instruction inst : proc.getCode().getInstructionList()) {
                int locn = currLocn;
                inst.loadInstruction(this);
                if (listing) {
                    printListing(locn, inst);
                }
            }
        }
    }

//***************************** Public Methods *************************

    /**
     * Specify whether code tracing is to be output when executing
     */
    public void setTracing(EnumSet<Trace> flags) {
        tracing = flags;
    }

    /**
     * Begin executing the code stored in the stack machine.
     * Runs until a STOP opcode, a return to 0, or an illegal condition
     * e.g., popping an empty stack.
     */
    public void run() {
        running = true;
        /* Establish stack frame for the main program
         * Place dummy static and dynamic links on stack.
         * The stack machine begins execution with the frame pointer
         * equal to the stack pointer (both 0).
         * Hence the first value pushed is at the location
         * addressed by the frame pointer (fp).
         */
        push(0); // Push dummy static link for main program
        push(0); // Push dummy dynamic link for main program
        push(0); //Push return address for main program
        while (running) {
            execInstruction();
        }
        // dumpStack();
    }

//*********************** Public Code Generators ************************

    /**
     * Store the given word (with associated name) into the code buffer
     *
     * @param word to be stored
     */
    public void generateWord(int word) {
        if (currLocn >= MEM_LIMIT) {
            errors.error("Object code too large.", ErrorHandler.NO_LOCATION);
        } else {
            memory[currLocn++] = word;
        }
    }

    /**
     * Print a listing line to the message handler
     */
    private void printListing(int locn, Instruction inst) {
        /* Offset used in listing code */
        final int ASSEMBLY_POSN = 4;
        StringBuffer buf = new StringBuffer();
        pad(buf, ASSEMBLY_POSN);
        if (!(inst instanceof Instruction.CommentInstruction)) {
            buf.append(locn);
            pad(buf, ASSEMBLY_POSN + 5);
            buf.append(":");
        }
        pad(buf, ASSEMBLY_POSN + 7);
        buf.append(inst);
        outStream.println(buf.toString());
//        outStream.printf("%8d :  %s%n", locn, inst);
    }

//*********************** Run time auxiliary methods ********************

    /**
     * Format a value for printing
     */
    private String formatValue(int val) {
        return String.format("%d(x%x)", val, val);
    }
    /**
     * Push the value onto the stack, and increment the stack pointer
     */
    private void push(int val) {
        if (sp >= limit) {
            runtimeError("memory overflow!");
        } else {
            if (tracing.contains(Trace.STACK)) {
                outStream.print(" Push(" + formatValue(val) + ") ");
            }
            memory[sp++] = val;
        }
    }

    /**
     * Pop the top value form the stack and decrement the stack pointer
     */
    private int pop() {
        if (sp <= STACK_START) {
            runtimeError("stack underflow!");
            return 0;
        } else {
            if (tracing.contains(Trace.STACK)) {
                outStream.print(" Pop() = " + formatValue(memory[sp - 1]) + " ");
            }
            return memory[--sp];
        }
    }

    /**
     * Return value stored at address
     */
    private int loadValue(int address) {
        int val = 0;
        if (address < 0 || address >= MEM_LIMIT) {
            runtimeError("load outside memory pc=" +
                    +(pc - 1) + ": address=" + address);
        } else {
            val = memory[address];
        }
        if (tracing.contains(Trace.MEM)) {
            outStream.printf("%n    Load [" + address + "] => " +formatValue(val));
        }
        return val;
    }

    /**
     * Store value at StoreAdr
     */
    private void storeValue(int address, int value) {
        if (address < 0 || address >= CODE_START) {
            runtimeError("store outside memory pc=" +
                    +(pc - 1) + ": address=" + (address));
        } else {
            memory[address] = value;
        }
        if (tracing.contains(Trace.MEM)) {
            outStream.printf("%n    Store [" + (address) + "] <= " + formatValue(value));
        }
    }

    /**
     * Dump the contents of the stack to stdout.
     * Used for debugging.
     */
    private void dumpStack() {
        outStream.println();
        outStream.println("Stack pointer = " + sp);
        for (int i = STACK_START; i < sp; i++) {
            StringBuffer out = new StringBuffer();
            if (i == fp) {
                out.append(" FP: ");
            } else {
                out.append("     ");
            }
            int n = out.length();
            out.append(i);
            pad(out, n + 4);
            out.append(": ");
            out.append(memory[i]);
            outStream.println(out);
        }
    }

    /**
     * Trace back of procedure calls
     */
    public void traceBack() {
        /* Start trace back from current program counter and frame pointer */
        int tracePC = pc;
        int traceFP = fp;
        while (tracePC != 0) {
            Procedures.ProcedureCode proc = procedures.getProcedure(tracePC - 1);
            if (proc == null) {
                // if fp is 0 then in main program setup/finalisation code
                if (fp != 0) {
                    outStream.println("Trace back terminated early - " +
                            "PC " + tracePC + " out of valid range");
                    dumpStack();
                }
                return;
            }
            outStream.print("PC=" + tracePC + " in " + proc);
            outStream.print(" FP=" + traceFP);
            int staticLink = memory[traceFP];
            outStream.print(" SL=" + staticLink);
            // Dynamic link is at offset 1 from frame pointer
            int dynamicLink = memory[traceFP + 1];
            outStream.print(" DL=" + dynamicLink);
            // Return address is at offset 2
            outStream.println(" RA=" + memory[traceFP + 2]);
            for (SymEntry entry : proc.getLocals().getEntries()) {
                if (entry instanceof SymEntry.VarEntry) {
                    SymEntry.VarEntry varEntry = (SymEntry.VarEntry) entry;
                    int varSize = varEntry.getType().getBaseType().getSpace();
                    int addr = traceFP + varEntry.getOffset();
                    String varVal = "  " + varEntry.getIdent() +
                            "(" + varEntry.getOffset() + ")" + " =";
                    for (int i = 0; i < varSize; i++) {
                        if (0 <= addr && addr < CODE_START) {
                            varVal += " " + memory[addr];
                            addr++;
                        } else {
                            varVal += " offset out of stack bounds";
                            break;
                        }
                    }
                    outStream.println(varVal);
                }
            }
            // Return PC is at offset 2 from frame pointer
            tracePC = memory[traceFP + 2];
            if (dynamicLink != 0 && dynamicLink > traceFP - 3) {
                outStream.println("Trace back terminated early - " +
                        "invalid dynamic link " + dynamicLink + " FP= " + traceFP);
                dumpStack();
                return;
            }
            traceFP = dynamicLink;
        }
        outStream.println("End of traceBack");
    }

    /**
     * Right pad the given string buffer to the given length
     */
    private void pad(StringBuffer buf, int to) {
        for (int i = buf.length(); i < to; i++) {
            buf.append(' ');
        }
    }

    /**
     * Runtime error while executing program.
     */
    private void runtimeError(String message) {
        running = false;
        outStream.println("\nRuntime error: " + message);
        // dumpStack();
        if (tracing.contains(Trace.STACK)) {
            traceBack();
        }
    }
//********************************** Execution *******************************
    /**
     * Convert from integer to operation
     */
    private Operation[] getOperation = Operation.values();

    /**
     * Execute the instruction pointed to by the pc register,
     * and adjust pc to point to the next instruction.
     */
    private void execInstruction() {
        if (pc < CODE_START || pc >= currLocn) {
            runtimeError("PC = " + pc + " out of range of code");
            return;
        }
        int instWord = memory[pc++];
        if (instWord < 0 || getOperation.length <= instWord) {
            runtimeError("invalid opcode");
            return;
        }
        Operation inst = getOperation[instWord];
        int address;
        if (tracing.contains(Trace.STATE)) {
            String out;
            out = String.format("%nPC:%5d FP: %5d SP: %5d Limit: %5d Opcode: %s ",
                    pc-1, fp, sp, limit, inst);
            if (inst == Operation.LOAD_CON) {
                out += memory[pc] + " ";
            }
            outStream.print(out);
        }
        switch (inst) {
            case NO_OP: /* Do nothing */
                break;
            case BR: /* Unconditional branch */
                int dest = pop(); /* destination offset */
                pc += dest;       /* branch relative to pc */
                if (tracing.contains(Trace.JUMPS)) {
                    outStream.print("\n      Branch => " + pc);
                }
                break;
            case BR_FALSE: /* If the second top value = FALSE_VALUE,
                jump to the destination */
                dest = pop();
                int test = pop();
                if (test == Type.FALSE_VALUE) {
                    pc += dest;
                } else if (test != Type.TRUE_VALUE) {
                    runtimeError("non-boolean operand in branch");
                }
                if (tracing.contains(Trace.JUMPS)) {
                    outStream.print("\n      Branch => " + pc);
                }
                break;
            case BR_TRUE: /* If the second top value = TRUE_VALUE,
                jump to the destination */
                dest = pop();
                test = pop();
                if (test == Type.TRUE_VALUE) {
                    pc += dest;
                } else if (test != Type.FALSE_VALUE) {
                    runtimeError("non-boolean operand in branch");
                }
                if (tracing.contains(Trace.JUMPS)) {
                    outStream.print("\n      Branch => " + pc);
                }
                break;
            case COPY: /* Copy top-of-stack words from third-top-of-stack address
                      to second-top-of-stack address */
                int copySize = pop();
                int toAddr = fp + pop();
                int fromAddr = fp + pop();
                int copyLimit = fromAddr + copySize;
                while (fromAddr < copyLimit && running) {
                    storeValue(toAddr, loadValue(fromAddr));
                    fromAddr += 1;
                    toAddr += 1;
                }
                break;
            case CALL: /* Execute a call */
                int addr = pop();   /* pop address of procedure */
                /* Set up a new stack frame.
                 * We assume a static link has already been set up */
                push(fp);           /* push fp to create the dynamic link */
                fp = sp - 2;        /* frame pointer addresses static link */
                push(pc);           /* save return address */
                pc = addr;          /* branch to procedure */
                if (tracing.contains(Trace.CALLS)) {
                    Procedures.ProcedureCode proc = procedures.getProcedure(pc);
                    outStream.print("\n      Call => " + proc.getName() + " at " + pc);
                }
                break;
            case RETURN: /* Return to caller */
                sp = fp + 3;   /* Set stack pointer so next pop is return address
                              this will also deallocate any locals */
                pc = pop();    /* Set program counter to return address. */
                fp = pop();    /* Restore the frame pointer from dynamic link */
                pop();         /* Remove the static link */
                if (pc == 0) { /* Return from main terminates program */
                    running = false;
                }
                if (tracing.contains(Trace.CALLS)) {
                    if (pc == 0) {
                        outStream.println("\n      Exiting program");
                    } else {
                        outStream.print("\n      Returning to => " +
                                procedures.getProcedure(pc).getName() + " at " + pc);
                    }
                }
                break;
            case ALLOC_STACK: /* Allocate top-of-stack words on stack */
                /* It is assumed that the top of stack contains the number of
                 * words to be allocated on the stack. */
                int locs = pop(); /* size in words */
                if (locs < 0) {
                    runtimeError("allocating a negative number of locations on stack");
                }
                /* Allocate space on stack */
                for (int i = 1; i <= locs && running; i++) {
                    /* Push a useless value to make error detection more likely. */
                    push(NULL_ADDR);
                }
                break;
            case DEALLOC_STACK: /* Remove locations from the stack */
                int nwords = pop(); /* Number of words for to deallocate */
                if (nwords < 0) {
                    runtimeError("deallocating a negative number of locations on stack");
                }
                if (sp - nwords <= fp + 2) {
                    runtimeError("deallocating too many words");
                } else {
                    sp -= nwords;   /* Deallocate locations */
                }
                break;
            case POP: /* Discard the top of stack */
                pop();
                break;
            case DUP: /* Duplicate value on stack */
                int val = pop();
                push(val);
                push(val);
                break;
            case SWAP: /* Swap top two values on stack */
                int val1 = pop();
                int val2 = pop();
                push(val1);
                push(val2);
                break;
            case DIV: /* Divide */
                int divbottom = pop();
                int divtop = pop();
                if (divbottom == 0) {
                    runtimeError("divide by zero");
                } else {
                    push(divtop / divbottom);
                }
                break;
            case MPY: /* Multiply */
                push(pop() * pop());
                break;
            case ADD: /* Add */
                push(pop() + pop());
                break;
            case XOR: /* Bitwise XOR */
                push(pop() ^ pop());
                break;
            case OR: /* Bitwise OR */
                push(pop() | pop());
                break;
            case AND: /* Bitwise AND */
                push(pop() & pop());
                break;
            case SHIFT_LEFT: /* Shift second top left number of place in top */
                int shift_count = pop();
                int shift_val = pop();
                push(shift_val << shift_count);
                break;
            case SHIFT_RIGHT: /* Shift second top right number of place in top */
                shift_count = pop();
                shift_val = pop();
                push(shift_val >> shift_count);
                break;
            case EQUAL: /* Test if top two values are equal */
                push(pop() == pop() ? Type.TRUE_VALUE : Type.FALSE_VALUE);
                break;
            case LESS: /* Test if second top value < top value */
                int top = pop();
                int second = pop();
                push(second < top ? Type.TRUE_VALUE : Type.FALSE_VALUE);
                break;
            case LESSEQ: /* Test if second top value <= top value */
                top = pop();
                second = pop();
                push(second <= top ? Type.TRUE_VALUE : Type.FALSE_VALUE);
                break;
            case NOT: /* Bitwise inversion */
                push(~pop());
                break;
            case NEGATE: /* 2s complement */
                push(-pop());
                break;
            case READ: /* Read a number from stdin */
                int read;
                try {
                    read = Integer.parseInt(in.readLine());
                    push(read);
                } catch (Exception e) {
                    runtimeError("invalid value read - must be an integer");
                }
                break;
            case WRITE: /* Write a number to stdout */
                outStream.println(pop());
                break;
            case BOUND: /* Return true iff index is within bounds.
                This needs to be an instruction to write the error */
                int upper = pop();
                int lower = pop();
                val = pop();
                if (lower <= val && val <= upper) {
                    push(Predefined.BOOLEAN_TYPE.TRUE_VALUE); // in bounds
                } else {
                    push(Predefined.BOOLEAN_TYPE.FALSE_VALUE); // out of bounds
                }
                break;
            case TO_GLOBAL: /* Adjust local to global */
                push(pop() + fp);
                break;
            case TO_LOCAL: /* Adjust a global address to a frame-local one */
                push(pop() - fp);
                break;
            case LOAD_CON: /* Load a constant value from the following word */
                push(memory[pc++]);
                break;
            case LOAD_ABS: /* Load a value from address in top of stack */
                address = pop();
                push(loadValue(address));
                break;
            case STORE_FRAME: /* Store a value into memory */
                address = fp + pop();
                int value = pop();
                storeValue(address, value);
                break;
            case LOAD_FRAME: /* Load a value from memory frame relative */
                address = fp + pop();
                push(loadValue(address));
                break;
            case ZERO: /* Push 0 on the stack */
                push(0);
                break;
            case ONE: /* Push 1 on the stack */
                push(1);
                break;
            case ALLOC_HEAP: /* Allocate memory from heap */
                int size = pop();
                limit -= size;
                push(limit); // will fail if limit less than sp
                for (int i = limit; i < limit + size; i++) {
                    memory[i] = NULL_ADDR;
                }
                break;
            case LOAD_MULTI: /* Load multiple words onto stack from
                            address on second top of stack */
                int count = pop();        /* pop count of number of words */
                address = fp + pop();     /* address relative to frame pointer */
                while (count > 0) {
                    push(loadValue(address++));
                    count--;
                }
                break;
            case STORE_MULTI: /* Store multiple words from stack to
                             address on second top of stack */
                count = pop();        /* pop count of number of words */
                address = fp + pop() + count; /* relative to frame pointer */
                while (count > 0) {
                    /* store from last location back (to match LOAD_MULTI) */
                    storeValue(--address, pop());
                    count--;
                }
                break;
            case STOP: /* Halt */
                int exitcode = pop();
                switch (exitcode) {
                    case OUT_OF_BOUNDS:
                        runtimeError("expression out of bounds");
                        break;
                    default:
                        runtimeError("machine halted with code " + exitcode);
                        break;
                }
                break;
            default:
                runtimeError("opcode not implemented: " + inst);
        }
    }
}
