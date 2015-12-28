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
public class OperatorPrecedenceParser<E> implements Parser<E> {

	Function<String, Integer> getPrecedenceLevel;
	Function<String, Function<TokenList, E>> parsePrimary;
	Function<String, BinaryOperator<E>> construct;

	private OperatorPrecedenceParser() {}
	
	/**
	 * All customizations available
	 * 
	 * @param pl Should return the precedence level of the operator
	 * @param primaryParser Returns the RHS parser of the operator if applied with a string, otherwise the LHS parser
	 * @param constructor Should return the constructor function of the operator
	 * @return the new parser
	 */
	public static<E> OperatorPrecedenceParser<E> newCustomizedParser(Function<String, Integer> pl, Function<String, Function<TokenList, E>> primaryParser, Function<String, BinaryOperator<E>> constructor) {
		OperatorPrecedenceParser parser = new OperatorPrecedenceParser();
		parser.getPrecedenceLevel = pl;
		parser.construct = constructor;
		parser.parsePrimary = primaryParser;
		return parser;
	}
	
	/**
	 * Use this if all operators use the same RHS parser
	 * 
	 * @param pl Should return the precedence level of the operator
	 * @param constructor Should return the constructor function of the operator
	 * @param primaryParser The primary parser
	 * @return the new parser
	 */
	public static<E> OperatorPrecedenceParser<E> newBasicRHSParser(Function<String, Integer> pl, Function<String, BinaryOperator<E>> constructor, Function<TokenList, E> primaryParser) {
		OperatorPrecedenceParser parser = new OperatorPrecedenceParser();
		parser.getPrecedenceLevel = pl;
		parser.construct = constructor;
		parser.parsePrimary = str -> primaryParser;
		return parser;
	}
	
	/**
	 * Loads required functions from the operator library
	 * 
	 * @param library The library
	 * @return the new parser
	 */
	public static<E> OperatorPrecedenceParser<E> fromLibrary(OperatorLibrary<E> library) {
		return newCustomizedParser(
				library::getPrecedence,
				library::getRhsParser,
				library::getConstructor
		);
	}
	
	@Override
	public E parse(TokenList tl) throws ParsingException {
		return parse(tl, parsePrimary.apply(null).apply(tl));
	}
	
	/**
	 * Parses the operators
	 * 
	 * @param tl The token list
	 * @param lhs The first operand (not parsed by OPP)
	 * @return The parsed tree
	 */
	public E parse(TokenList tl, E lhs) {
		return _parse(tl, lhs, 0);
	}
	
	private E _parse(TokenList tl, E initialLhs, int level) {
		E lhs = initialLhs;
		
		int oplevel = 0;
		while ((oplevel=getPrecedenceLevel.apply(tl.seekString())) >= 0
				&& oplevel >= level) {
			String op = tl.nextString();
			E rhs = parsePrimary.apply(op).apply(tl);
			while (getPrecedenceLevel.apply(tl.seek().getToken()) > oplevel) {
				rhs = _parse(tl, rhs, getPrecedenceLevel.apply(tl.seekString()));
			}
			lhs = construct.apply(op).apply(lhs, rhs);
		}
		return lhs;
	}
}
