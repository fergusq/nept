package org.kaivos.nept.parser;

/**
 * Parses information using token list as the source
 * 
 * @author Iikka
 *
 * @param <E> The type which information is being processed to
 */
public interface Parser<E> {

	/**
	 * Parses information from the token list
	 * 
	 * @param tl The token list
	 * @return The information
	 * @throws ParsingException on syntax error
	 */
	public E parse(TokenList tl) throws ParsingException;
	
}
