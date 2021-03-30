package parse;

import java.util.EnumSet;

/**
 * class TokenSet - Provides operations on sets of Tokens
 * Provide operations to construct, union and test membership
 * of set of Tokens.
 */
public class TokenSet {

    private final EnumSet<Token> set;

    /**
     * Construct a new TokenSet from a list of tokens
     */
    public TokenSet(Token first, Token... rest) {
        set = EnumSet.of(first, rest);
    }

    /**
     * Construct a new TokenSet from an existing one
     */
    private TokenSet(TokenSet elems) {
        set = EnumSet.copyOf(elems.set);
    }

    /**
     * Construct a new TokenSet from the union of this and the other
     */
    TokenSet union(TokenSet other) {
        TokenSet result = new TokenSet(other);
        result.set.addAll(set);
        return result;
    }

    /**
     * Construct a new TokenSet from this plus one more Token
     */
    TokenSet union(Token other) {
        TokenSet result = new TokenSet(other);
        result.set.addAll(set);
        return result;
    }

    /**
     * Construct a new TokenSet from this plus a list of Tokens
     */
    TokenSet union(Token first, Token... rest) {
        TokenSet result = new TokenSet(first, rest);
        result.set.addAll(set);
        return result;
    }

    /**
     * Return whether a token is contained in the set
     */
    boolean contains(Token token) {
        return set.contains(token);
    }

    /**
     * Convert set to string
     */
    @Override
    public String toString() {
        StringBuilder m = new StringBuilder("{ ");
        String sep = "";
        for (Token t : set) {
            m.append(sep).append("'").append(t).append("'");
            sep = ", ";
        }
        return m + " }";
    }
}
