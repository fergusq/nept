package org.kaivos.nept.parser;

import java.util.Arrays;
import java.util.List;

/**
 * A wrapper for a <code>List&lt;Token&gt;</code>
 * 
 * @author Iikka Hauhio
 *
 */
public class TokenList {

	private List<Token> tokens;
	private int index;
	
	/**
	 * The default constructor
	 * 
	 * @param tokens The list of tokens
	 */
	public TokenList(List<Token> tokens) {
		this.tokens = tokens;
		this.index = 0;
	}
	
	/**
	 * Returns the next token from the list and increases the index counter
	 * 
	 * @return The next token
	 * @throws IndexOutOfBoundsException If no tokens left
	 */
	public Token next() {
		return tokens.get(index++);
	}
	
	/**
	 * Returns the next tokenas a string from the list and increases the index counter
	 * 
	 * @return The next token
	 * @throws IndexOutOfBoundsException If no tokens left
	 */
	public String nextString() {
		return tokens.get(index++).getToken();
	}
	
	/**
	 * Lookahead, returns the next token from the list, equivalent to <code>seek(0)</code>
	 * 
	 * @return The next token
	 * @throws IndexOutOfBoundsException If no tokens left
	 */
	public Token seek() {
		return tokens.get(index);
	}
	
	/**
	 * Lookahead
	 * 
	 * @param n
	 * @return The next token
	 * @throws IndexOutOfBoundsException If no tokens left
	 */
	public Token seek(int n) {
		return tokens.get(index+n);
	}
	
	/**
	 * Lookahead, returns the next token as a string from the list, equivalent to <code>seekString(0)</code>
	 * 
	 * @return The next token
	 * @throws IndexOutOfBoundsException If no tokens left
	 */
	public String seekString() {
		return tokens.get(index).getToken();
	}
	
	/**
	 * Lookahead, returns a string
	 * 
	 * @param n
	 * @return The next token
	 * @throws IndexOutOfBoundsException If no tokens left
	 */
	public String seekString(int n) {
		return tokens.get(index+n).getToken();
	}
	
	/**
	 * Are there any tokens left?
	 * 
	 * @return <code>true</code> if tokens left, otherwise <code>false</code>
	 */
	public boolean hasNext() {
		return index < tokens.size();
	}
	
	/**
	 * Compares the next token to a keyword
	 * 
	 * @param keyword The keyword
	 * @return <code>true</code> or <code>false</code>
	 */
	public boolean isNext(String keyword) {
		return hasNext() ? seek().getToken().equals(keyword) : false;
	}
	
	/**
	 * Accepts a keyword
	 * 
	 * @param keyword
	 * @throws ParsingException if unexpected token was encountered
	 * @throws IndexOutOfBoundsException If no tokens left
	 */
	public void accept(String... keyword) throws ParsingException {
		Token next = next();
		if (!Arrays.asList(keyword).contains(next.getToken()))
			throw new ParsingException("Expected `" + keyword + "'", next);
	}
	
	/**
	 * Resets the index counter
	 */
	public void reset() {
		index = 0;
	}
	
	/**
	 * Appends a token to the end of the list
	 * 
	 * @param token The token
	 * @return self
	 */
	public TokenList append(Token token) {
		tokens.add(token);
		return this;
	}
	
	@Override
	public String toString() {
		return tokens.toString();
	}
	
}
