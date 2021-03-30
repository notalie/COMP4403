package interpreter;

import syms.SymEntry;

/**
 * Frame stores variables and their associated values for a procedures scope.
 * Additionally tracks the static level and static and dynamic links
 */
class Frame {
    /**
     * Dynamic link of the frame
     */
    private final Frame dynamicLink;

    /**
     * Static link of the frame
     */
    private final Frame staticLink;

    /**
     * Static level of the frame
     */
    private final int level;

    /**
     * The procedure for this frame
     */
    private final SymEntry.ProcedureEntry procedure;

    /**
     * Variable assignments - null entries in the array
     * represent unassigned variables
     */
    private final Value[] entries;

    /**
     * This constructs a single scope within a symbol table
     * that is linked to the parent scope.
     *
     * @param dynamicLink of frame, null to indicate no parent
     * @param staticLink  of frame, null to indicate no parent
     * @param procedure   proc entry of this frame
     */
    Frame(Frame dynamicLink, Frame staticLink,
          SymEntry.ProcedureEntry procedure) {
        this.dynamicLink = dynamicLink;
        this.staticLink = staticLink;
        this.level = procedure.getLocalScope().getLevel();
        this.procedure = procedure;

        /* Initially all entries are unassigned (represented by null) */
        this.entries = new Value[procedure.getLocalScope().getVariableSpace()];
    }

    /**
     * Enter a new frame with this as the parent
     */
    Frame enterFrame(SymEntry.ProcedureEntry procedure) {
        /* Find the static link for the new level */
        Frame newStaticLink = lookupFrame(procedure.getLevel());

        return new Frame(this, newStaticLink, procedure);
    }

    /**
     * Exit the frame by returning the parent frame
     */
    Frame exitFrame() {
        return dynamicLink;
    }

    /**
     * Find the frame at newLevel by following static links
     */
    Frame lookupFrame(int newLevel) {
        Frame foundFrame = this;
        int i = this.level;

        /* Follow static links to find the new static link */
        while (newLevel < i) {
            foundFrame = foundFrame.staticLink;
            i--;
        }

        return foundFrame;
    }

    /**
     * Lookup variable in this frame with a given offset.
     *
     * @param offset offset of the variable to lookup
     * @return value stored at the given offset, entry. null if not assigned.
     */
    Value lookup(int offset) {
        return entries[offset];
    }

    /**
     * Assign the value to the provided offset.
     *
     * @param offset of the variable within the frame.
     * @param value  to assign to the offset.
     */
    void assign(int offset, Value value) {
        entries[offset] = value;
    }

    /**
     * Dump contents of this frame
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("PROC ");
        result.append(procedure.getIdent()).append(" : level ")
                .append(level).append(System.lineSeparator());

        for (SymEntry entry : procedure.getLocalScope().getEntries()) {
            if (!(entry instanceof SymEntry.VarEntry)) {
                continue;
            }

            SymEntry.VarEntry variable = (SymEntry.VarEntry) entry;
            Value value = entries[variable.getOffset()];

            result.append("\t").append(variable.getIdent());

            if (value == null) {
                result.append(" = unassigned").append(System.lineSeparator());
                continue;
            }

            result.append(" = ").append(value);
            result.append(System.lineSeparator());
        }
        if (dynamicLink != null) {
            result.append(dynamicLink.toString());
        }
        return result.toString();
    }
}

