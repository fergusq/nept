package org.kaivos.nept.parser;

import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Parses operators 
 * 
 * @author Iikka Hauhio
 *
 * @param <E> The type of the primary tree object
 */
public class OperatorPrecedenceParser<E> {

	Function<String, Integer> getPrecedenceLevel;
	Function<String, E> parsePrimary;
	Function<String, BinaryOperator<E>> construct;
	
	public OperatorPrecedenceParser(Function<String, Integer> pl, Function<String, E> primaryParser, Function<String, BinaryOperator<E>> constructor) {
		getPrecedenceLevel = pl;
		construct = constructor;
		parsePrimary = primaryParser;
	}
	
	public OperatorPrecedenceParser(Function<String, Integer> pl, Function<String, BinaryOperator<E>> constructor, Supplier<E> primaryParser) {
		getPrecedenceLevel = pl;
		construct = constructor;
		parsePrimary = str -> primaryParser.get();
	}
	
	public OperatorPrecedenceParser(OperatorLibrary<E> library) {
		this(
				library::getPrecedence,
				library::parseRhs,
				library::getConstructor
		);
	}
	
	public E parse(TokenList tl, E lhs) {
		return _parse(tl, lhs, 0);
	}
	
	private E _parse(TokenList tl, E lhs, int level) {
		int oplevel = 0;
		while ((oplevel=getPrecedenceLevel.apply(tl.seekString())) >= 0
				&& oplevel >= level) {
			String op = tl.nextString();
			E rhs = parsePrimary.apply(op);
			while (getPrecedenceLevel.apply(tl.seek().getToken()) > oplevel) {
				rhs = _parse(tl, rhs, getPrecedenceLevel.apply(tl.seekString()));
			}
			lhs = construct.apply(op).apply(lhs, rhs);
		}
		return lhs;
	}
}
