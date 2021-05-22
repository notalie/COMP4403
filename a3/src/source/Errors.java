package source;

import java_cup.runtime.ComplexSymbolFactory.Location;

/**
 * interface Errors - interface to allow reporting of compilation
 * errors and other messages. Use flush() to cause output.
 */
public interface Errors {

    /**
     * Signal an error at the given location
     */
    void error(String m, Location loc);

    /**
     * Signal a fatal error at the given location
     */
    void fatal(String m, Location loc);

    /**
     * Output debugging message or error
     */
    void debugPrint(String msg);

    /**
     * Output debugging message if debug turned on
     */
    void debugMessage(String msg);

    /**
     * Increment debug level for indenting messages
     */
    void incDebug();

    /**
     * Decrement debug level for indenting messages
     */
    void decDebug();

    /**
     * Report error is assert condition fails
     */
    void checkAssert(boolean condition, String m, Location loc);

    /**
     * Print immediately a summary of all errors reported
     */
    void errorSummary();

    /**
     * List impending error messages, and clear accumulated errors.
     */
    void flush();

    /**
     * Return whether any errors have been reported at all
     */
    boolean hadErrors();

    /**
     * Print line to output stream
     */
    void println(String msg);

}
