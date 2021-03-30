package parse;

import source.ErrorHandler;
import source.Errors;
import java_cup.runtime.ComplexSymbolFactory.Location;

/**
 * LexicalToken - Defines the basic tokens returned from the lexical analyser.
 * Arguably should be an inner class of Scanner, but is a little
 * easier this way. Also gets heavily used by Parser.
 */

public class LexicalToken {

    /**
     * Kind of token, e.g. IDENTIFIER, NUMBER, PLUS
     */
    private final Token kind;
    /**
     * The location of the first char of the token in the input source
     */
    private final Location loc;
    /**
     * Error handler
     */
    private static final Errors errors = ErrorHandler.getErrorHandler();

//****************** Constructors ********************

    /**
     * Construct a token with the given type and location.
     *
     * @param kind Type of the lexical token
     * @param loc  Location in the source input file
     */
    public LexicalToken(Token kind, Location loc) {
        this.kind = kind;
        this.loc = loc;
    }

//****************** Public Methods ******************

    /**
     * Extract the type of a token
     */
    Token getKind() {
        return kind;
    }

    /**
     * Extract the location of a token
     */
    public Location getLocation() {
        return loc;
    }

    /**
     * Test if the type of the token matches the argument
     *
     * @param kind Type to be matched
     */
    boolean isMatch(Token kind) {
        return this.kind == kind;
    }

    /**
     * Don't want to accidentally use equals instead of isMatch
     */
    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object o) {
        errors.fatal("Use isMatch to compare token kind", loc);
        return false;
    }

    /**
     * Test if the token is contained in the given set of token types
     *
     * @param tokenTypes set of tokens to test against
     * @return true iff in the set
     */
    boolean isIn(TokenSet tokenTypes) {
        return tokenTypes.contains(this.kind);
    }

    /* Virtual extract integer value of INTEGER token */
    public int getIntValue() {
        errors.fatal("call on getIntValue on a Token", loc);
        return 0;
    }

    /* Virtual extract name of IDENTIFIER token */
    public String getName() {
        errors.fatal("Internal error: call on getName on a Token", loc);
        return null;
    }

    /**
     * Return token name and location as debug string
     */
    public String toString() {
        return "'" + kind.toString() + "'";
//              + " at line " + (getLocation().getLine()+1) 
//              + " column " + getLocation().getColumn();
    }
}
