package interpreter;

import source.ErrorHandler;
import source.Errors;

public abstract class Value {

    /**
     * It is a fatal error if getInteger is called on other than an IntegerValue
     * @return never
     */
    public int getInteger(){
        fatal("Internal error in Value.getInteger");
        return -1; //never gets here
    }
    /**
     * It is a fatal error if getAddressFrame is called on other than an AddressValue
     * @return never
     */
    public int getAddressLevel(){
        fatal("Internal error in Value.getAddressFrame");
        return -1; // never gets here
    }
    /**
     * It is a fatal error if getAddressOffset is called on other than an AddressValue
     * @return never
     */
    public int getAddressOffset(){
        fatal("Internal error in Value.getAddressOffset");
        return -1; // never gets here
    }

    public static class IntegerValue extends Value {
        private int value;

        public IntegerValue(int value) {
            this.value = value;
        }

        public int getInteger() {
            return value;
        }

        public String toString() {
            return Integer.toString(value);
        }
    }

    public static class AddressValue extends Value {
        private int level;
        private int offset;

        public AddressValue(int level, int offset) {
            this.level = level;
            this.offset = offset;
        }

        public int getAddressLevel() {
            return level;
        }

        public int getAddressOffset() {
            return offset;
        }

        public String toString() {
            return "address(" + level + "," + offset + ")";
        }
    }

    public static class ErrorValue extends Value {
    }

    private Errors errors = ErrorHandler.getErrorHandler();
    /**
     * Signal a runtime error has occurred at a given location
     */
    private void fatal(String m) {
        errors.fatal(m, ErrorHandler.NO_LOCATION);
    }

}
