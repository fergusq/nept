package org.kaivos.nept.parser;

import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.util.stream.Collectors;

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
	public void shift() {
		index++;
		if (index >= tokens.size())
			throw new IndexOutOfBoundsException();
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
	 * @param n skips n number of elements
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
	 * @param n skips n number of elements
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
	 * Are there given number of tokens left?
	 * 
	 * @param number the number of tokens
	 * @return <code>true</code> if tokens left, otherwise <code>false</code>
	 */
	public boolean has(int number) {
		return index + number-1 < tokens.size();
	}
	
	/**
	 * Compares the next token to some keywords
	 * 
	 * @param keyword The keyword(s)
	 * @return <code>true</code> or <code>false</code>
	 */
	public boolean isNext(String... keyword) {
		return hasNext() ? Arrays.asList(keyword).contains(seek().getToken()) : false;
	}
	
	/**
	 * Accepts a keyword
	 * 
	 * @param keyword The list of acceptable keywords
	 * @return The accepted keyword
	 * @throws ParsingException if unexpected token was encountered
	 * @throws IndexOutOfBoundsException If no tokens left
	 */
	public Token accept(String... keyword) throws ParsingException {
		Token next = next();
		if (!Arrays.asList(keyword).contains(next.getToken()))
			throw new ParsingException(expected(keyword), next);
		return next;
	}

	/**
	 * Compares the next token to some keywords. If one of them
	 * matches, it will be accepted.
	 * 
	 * @param keyword The keyword(s)
	 * @return <code>true</code> if a keyword was accepted, otherwise <code>false</code>
	 */
	public boolean acceptIfNext(String... keyword) {
		boolean isItNext = isNext(keyword);
		if (isItNext) shift();
		return isItNext;
	}
	
	/**
	 * Returns a string usable in error messages
	 * 
	 * @param keyword The keywords
	 * @return The error string
	 */
	public static String expected(String... keyword) {
		if (keyword.length == 1)
			return "Expected `" + keyword[0] + "'";
		return "Expected one of `"
				+ Arrays.asList(keyword).stream()
					.limit(keyword.length-1)
					.collect(Collectors.joining("', `"))
				+ "' or `" + keyword[keyword.length-1] + "'";
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

	/**
	 * Returns the tokens as a List
	 * @return list
	 */
	public List<Token> toList() {
		return Collections.unmodifiableList(tokens);
	}
	
	@Override
	public String toString() {
		return tokens.toString();
	}
	
}
