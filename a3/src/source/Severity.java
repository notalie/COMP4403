package source;

/**
 * enumeration Severity - Error message severity types
 */
public enum Severity {
    FATAL("Fatal"),
    ERROR(" Error");

    private final String message;

    Severity(String message) {
        this.message = message;
    }

    public String toString() {
        return message;
    }
}
