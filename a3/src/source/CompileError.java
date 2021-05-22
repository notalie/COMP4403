package source;

import java_cup.runtime.ComplexSymbolFactory.Location;

/**
 * class CompilerError -  Represents a single error.
 * An error can consist of
 * - an error message string,
 * - the severity, and
 * - the location in the source input of the error.
 * If no location can be assigned to the error then
 * ErrorHandler.NO_LOCATION is used.
 *
 * @see source.Severity
 */
public class CompileError implements Comparable<CompileError> {
    /**
     * The error message
     */
    private final String message;
    /**
     * The error's severity
     */
    private final Severity severity;
    /**
     * The location in the input source, or NO_LOCATION
     */
    private final Location location;

    public CompileError(String message, Severity severity, Location loc) {
        this.message = message;
        this.severity = severity;
        this.location = loc;
    }

    /**
     * Ordering of errors is based on their location.
     *
     * @see java.lang.Comparable#compareTo
     */
    public int compareTo(CompileError that) {
        if (location.getLine() < that.getLocation().getLine()) {
            return -1;
        } else if (location.getLine() == that.getLocation().getLine()) {
            return Integer.compare(location.getColumn(), that.getLocation().getColumn());
        } else {
            return 1;
        }
    }

    public Location getLocation() {
        return location;
    }

    Severity getSeverity() {
        return severity;
    }

    public String toString() {
        return severity.toString() + ": " + message;
    }
}
