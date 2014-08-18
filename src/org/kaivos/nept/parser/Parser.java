package org.kaivos.nept.parser;

public interface Parser<E> {

	public E parse(TokenList tl) throws ParsingException;
	
}
