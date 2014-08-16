package org.kaivos.nept.parser;

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
	 * Resets the index counter
	 */
	public void reset() {
		index = 0;
	}
	
	@Override
	public String toString() {
		return tokens.toString();
	}
	
}
