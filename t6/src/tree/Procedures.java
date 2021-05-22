package tree;

import java.util.LinkedList;
import java.util.List;

import machine.StackMachine;
import source.ErrorHandler;
import syms.Scope;
import syms.SymEntry;

/**
 * class Procedures - code for each procedure and start and finish
 * addresses. Handles a stack trace back for the stack machine
 * in the event of a runtime error.
 */
public class Procedures {

    /**
     * List of procedure starts
     */
    private final List<ProcedureCode> procEntries;
    /**
     * Current code location for tracking start addresses of procedures
     */
    private int current;

    public class ProcedureCode {
        final SymEntry.ProcedureEntry procEntry;
        final Code code;
        final int finish;

        ProcedureCode(SymEntry.ProcedureEntry procEntry, Code code) {
            super();
            this.procEntry = procEntry;
            this.code = code;
            procEntry.setStart(current);
            current += code.size();
            this.finish = current;
        }

        public String getName() {
            return procEntry.getIdent();
        }

        public Scope getLocals() {
            return procEntry.getLocalScope();
        }

        public Code getCode() {
            return code;
        }

        @Override
        public String toString() {
            return procEntry + " : " + finish;
        }
    }

    public Procedures() {
        procEntries = new LinkedList<>();
        current = StackMachine.CODE_START;
    }

    public List<ProcedureCode> getProcedureEntries() {
        return procEntries;
    }

    public void addProcedure(SymEntry.ProcedureEntry procEntry, Code code) {
        procEntries.add(new ProcedureCode(procEntry, code));
    }

    public ProcedureCode getProcedure(int pc) {
        if (pc < StackMachine.CODE_START || current <= pc) {
            // Must be in main program setup or finalization code
            return null;
        }
        for (ProcedureCode ps : procEntries) {
            if (pc < ps.finish) {
                return ps;
            }
        }
        // Can't get here
        ErrorHandler.getErrorHandler().fatal(
                "getProcedure failed assertion 2: pc = " + pc, ErrorHandler.NO_LOCATION);
        return null;
    }

    @Override
    public String toString() {
        StringBuilder s = new StringBuilder();
        for (ProcedureCode start : procEntries) {
            s.append(start.toString()).append("\n");
        }
        return s.toString();
    }
}
