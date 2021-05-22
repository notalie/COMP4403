package machine;

import syms.SymEntry;

/**
 * class Instruction - represents an instruction in generated code
 */
public class Instruction {
    final Operation op;

    public Instruction(Operation op) {
        this.op = op;
    }

    public void loadInstruction(StackMachine machine) {
        machine.generateWord(op.ordinal());
    }

    @Override
    public String toString() {
        return op.toString();
    }

    /**
     * LOAD_CON is the only instruction with a parameter
     */
    public static class LoadConInstruction extends Instruction {
        protected int value;

        public LoadConInstruction(int value) {
            super(Operation.LOAD_CON);
            this.value = value;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        @Override
        public void loadInstruction(StackMachine machine) {
            super.loadInstruction(machine);
            machine.generateWord(value);
        }

        @Override
        public String toString() {
            return super.toString() + "(" + value + ")";
        }
    }

    /**
     * The addresses of procedures are resolved when the program is
     * loaded and the sizes of procedures are known.
     * The LOAD_CON is for the procedure address (eventually) and
     * hence this class extends LoadConInstruction.
     */
    public static class ProcRefInstruction extends LoadConInstruction {
        private final SymEntry.ProcedureEntry proc;

        public ProcRefInstruction(SymEntry.ProcedureEntry proc) {
            super(StackMachine.NULL_ADDR);
            this.proc = proc;
        }

        @Override
        public void loadInstruction(StackMachine machine) {
            value = proc.getStart();
            super.loadInstruction(machine);
        }

        @Override
        public String toString() {
            return op.toString() + "(" + proc.getIdent() +
                    (value == StackMachine.NULL_ADDR ? "" : ("," + value)) + ")";
        }
    }
}
