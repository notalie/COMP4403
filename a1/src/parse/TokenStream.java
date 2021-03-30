package parse;

import java.util.Stack;

import source.ErrorHandler;
import source.Errors;
import java_cup.runtime.ComplexSymbolFactory.Location;

public class TokenStream {

    //*************************** Instance Variables ************************
    /**
     * The lexical analyzer
     */
    private final Scanner lex;
    /**
     * The current token
     */
    private LexicalToken currentToken;
    /**
     * Track the non-terminal rule currently being parsed (for debugging)
     */
    private final Stack<String> ruleStack;
    /**
     * The object to report errors to
     */
    private final Errors errors = ErrorHandler.getErrorHandler();

    /**
     * Construct a token stream for the lexical analyser
     */
    public TokenStream(Scanner lex) {
        this.lex = lex;
        ruleStack = new Stack<>();
        currentToken = lex.next();      /* Initialise with first token */
    }

    /**
     * Get the kind of the current token
     */
    Token getKind() {
        return currentToken.getKind();
    }

    /**
     * Get the location of the current token
     */
    public Location getLocation() {
        return currentToken.getLocation();
    }

    /**
     * Get the name associated with the current token
     * requires currentToken.kind == Token.IDENTIFIER
     */
    public String getName() {
        assert currentToken.getKind() == Token.IDENTIFIER;
        return currentToken.getName();
    }

    /**
     * Get the integer value associated with the current token
     * requires currentToken.kind == Token.NUMBER
     */
    int getIntValue() {
        assert currentToken.getKind() == Token.NUMBER;
        return currentToken.getIntValue();
    }

    /**
     * Check if current token matches given token
     *
     * @param expected type of token expected to match current token
     */
    boolean isMatch(Token expected) {
        return currentToken.isMatch(expected);
    }

    /**
     * Check if current token matches any of the set of tokens
     *
     * @param tokenTypes set of token types expected to be matched
     */
    boolean isIn(TokenSet tokenTypes) {
        return currentToken.isIn(tokenTypes);
    }

    /**
     * Match if token is known to be expected, otherwise there is an error in
     * the parser. This version is used to move on to the next token and give
     * debugging output if enabled.
     *
     * @param expected - token expected next in the input stream.
     */
    public void match(Token expected) {
        errors.checkAssert(currentToken.isMatch(expected),
                "Match assertion failed on " + expected, getLocation());
        debugMessage("Matched " + currentToken.toString());
        currentToken = lex.next();
    }

    /**
     * Match a token equal to that expected.
     * If the current token is the expected token, it is skipped,
     * otherwise an error is reported and error recovery attempted.
     * For the error recovery, if the current token can follow the expected
     * token, then it is assumed that the expected token was omitted and
     * no error recovery is necessary, otherwise the current token is skipped.
     * If the current token is skipped then the next token may be the
     * expected token, if so, it is matched.
     *
     * @param expected - token expected next in the input stream.
     * @param follows  - set of tokens expected to follow the expected token.
     *                 requires follows is nonempty
     */
    public void match(Token expected, TokenSet follows) {
        if (currentToken.isMatch(expected)) {
            match(expected);
        } else {
            parseError("Parse error, expecting '" + expected + "'" + " in " +
                    ruleStack.peek());
            /* If the current token may follow the expected token then
             * treat it as though the expected token was missing and
             * do no further error recovery.
             */
            if (!currentToken.isIn(follows) &&
                    !currentToken.isMatch(Token.EOF)) {
                // Skip the erroneous token
                debugMessage("Skipping " + currentToken.toString());
                currentToken = lex.next();
                /* If after skipping, the (new) token is not the expected
                 * token we do no further error recovery (in match at least).
                 */
                if (currentToken.isMatch(expected)) {
                    /* If after skipping the erroneous token we find
                     * the expected token we match it
                     */
                    match(expected);
                }
            }
        }
    }

    /**
     * Match when follow set is a single token
     *
     * @param expected - token expected next in the input stream.
     * @param follows  - single token that may follow
     */
    public void match(Token expected, Token follows) {
        match(expected, new TokenSet(follows));
    }

    /**
     * Skip tokens until one is found which is in the parameter set find.
     * Used for error recovery.
     *
     * @param find - set of tokens: skip until one found in this set
     *             requires find.contains(Token.EOF);
     */
    private void skipTo(TokenSet find) {
        while (!currentToken.isIn(find)) {
            debugMessage("Skipping " + currentToken.toString());
            currentToken = lex.next();
        }
    }

    /**
     * Begin a parsing rule.
     * Ensure that the next token is in the set of tokens expected
     * at the start of a grammar rule. An error is reported if it isn't.
     * If the current token is not one of the expected tokens, skip until
     * either an expected token or a token in the recoverSet is found.
     * If an expected token is eventually found then return successfully
     * (true) otherwise fail. A successful beginRule increments the
     * indentation level for debugging messages.
     *
     * @param rule       - name of the rule for use in error messages
     * @param expected   - set of tokens expected at start of rule
     * @param recoverSet - set of tokens to recover at on a syntax error
     * @return true iff an expected token was (eventually) found.
     * requires recoverSet.contains(Token.EOF)
     */
    boolean beginRule(String rule, TokenSet expected,
                      TokenSet recoverSet) {
        debugMessage("Begin parse " + rule + " recover on " + recoverSet);
        if (!currentToken.isIn(expected)) {
            parseError(currentToken + " cannot start " + rule);
            /* skipping cannot fail as recoverSet contains end-of-file */
            skipTo(recoverSet.union(expected));
            if (!currentToken.isIn(expected)) {
                return false;
            }
        }
        /* Increase the indentation level if successful return */
        debugPush(rule);
        return true;
    }

    /**
     * End a parsing rule.
     * Ensure that the current token is a member of the recovery set
     * (i.e., something which an ancestor rule is expecting).
     *
     * @param rule       name of the rule for use in error messages
     * @param recoverSet set of tokens to recover at on a syntax error
     *                   requires recoverSet.contains(Token.EOF);
     */
    void endRule(String rule, TokenSet recoverSet) {
        String popped = debugPop(); /* Decrease debugging level at end of rule */
        if (!popped.equals(rule)) {
            debugMessage("<<<<< End rule " + rule +
                    " does not match start rule " + popped);
        }
        debugMessage("End parse " + rule);
    }
    //**************************** Support Methods ***************************

    /**
     * Push current rule onto debug rule stack and increase debug level
     */
    private void debugPush(String rule) {
        ruleStack.push(rule);
        errors.incDebug();
    }

    /**
     * Pop current rule from debug rule stack and decrease debug level
     */
    private String debugPop() {
        errors.decDebug();
        return ruleStack.pop();
    }

    /**
     * Output debugging message if debug turned on
     */
    private void debugMessage(String msg) {
        errors.debugMessage(msg);
    }

    /**
     * Error message handle for parsing errors
     */
    private void parseError(String msg) {
        errors.debugMessage(msg);
        errors.error(msg, currentToken.getLocation());
    }
}
