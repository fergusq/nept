package org.kaivos.nept.parser;

import java.util.HashMap;
import java.util.function.BinaryOperator;
import java.util.function.Supplier;
import java.util.function.Function;

/**
 * A database for operators
 * 
 * @author Iikka Hauhio
 *
 * @param <E> The type of the primary tree object
 */
public class OperatorLibrary<E> {

	private HashMap<String, BinaryOperator<E>> constructors = new HashMap<>();
	private HashMap<String, Integer> precedence = new HashMap<>();
	private HashMap<String, Function<TokenList, E>> rhsParsers = new HashMap<>();
	private Function<TokenList, E> defaultRhsParser;
	private int level = 0;
	
	/**
	 * The constructor
	 * 
	 * @param defaultRhsParser The default RHS parser
	 */
	public OperatorLibrary(Supplier<E> defaultRhsParser) {
		this.defaultRhsParser = tl -> defaultRhsParser.get();
	}

	/**
	 * The constructor
	 * 
	 * @param defaultRhsParser The default RHS parser
	 */
	public OperatorLibrary(Function<TokenList, E> defaultRhsParser) {
		this.defaultRhsParser = defaultRhsParser;
	}
	
	/**
	 * Adds a new operator to the current precedence level
	 * 
	 * @param op The operator
	 * @param handler The constructor function
	 */
	public void add(String op, BinaryOperator<E> handler) {
		add(op, level, handler);
	}
	
	/**
	 * Adds a new operator to the current precedence level
	 * 
	 * @param op The operator
	 * @param rhsParser The right-side parser function
	 * @param handler The constructor function
	 */
	public void add(String op, Supplier<E> rhsParser, BinaryOperator<E> handler) {
		add(op, level, tl -> rhsParser.get(), handler);
	}

	/**
	 * Adds a new operator to the current precedence level
	 * 
	 * @param op The operator
	 * @param rhsParser The right-side parser function
	 * @param handler The constructor function
	 */
	public void add(String op, Function<TokenList, E> rhsParser, BinaryOperator<E> handler) {
		add(op, level, rhsParser, handler);
	}
	
	/**
	 * Adds a new operator
	 * 
	 * @param op The operator
	 * @param precedenceLevel The precedence level
	 * @param handler The constructor function
	 */
	public void add(String op, int precedenceLevel, BinaryOperator<E> handler) {
		add(op, precedenceLevel, defaultRhsParser, handler);
	}
	
	/**
	 * Adds a new operator
	 * 
	 * @param op The operator
	 * @param precedenceLevel The precedence level
	 * @param rhsParser The right-side parser function
	 * @param handler The constructor function
	 */
	public void add(String op, int precedenceLevel, Supplier<E> rhsParser, BinaryOperator<E> handler) {
		constructors.put(op, handler);
		precedence.put(op, precedenceLevel);
		rhsParsers.put(op, tl -> rhsParser.get());
	}

	/**
	 * Adds a new operator
	 * 
	 * @param op The operator
	 * @param precedenceLevel The precedence level
	 * @param rhsParser The right-side parser function
	 * @param handler The constructor function
	 */
	public void add(String op, int precedenceLevel, Function<TokenList, E> rhsParser, BinaryOperator<E> handler) {
		constructors.put(op, handler);
		precedence.put(op, precedenceLevel);
		rhsParsers.put(op, rhsParser);
	}
	
	/**
	 * Increases the current precedence level
	 */
	public void increaseLevel() {
		level++;
	}
	
	/**
	 * Searches the precedence of the operator
	 * 
	 * @param op The operator
	 * @return The precedence if operator exists, otherwise -1
	 */
	public int getPrecedence(String op) {
		return precedence.containsKey(op) ? precedence.get(op) : -1;
	}
	
	/**
	 * Applies the constructor function of the operator with a and b
	 * 
	 * @param op The operator
	 * @param a The a operand
	 * @param b The b operand
	 * @return The return value of the constructor function
	 */
	public E construct(String op, E a, E b) {
		return constructors.get(op).apply(a, b);
	}
	
	/**
	 * Returns the constructor function of the operator
	 * 
	 * @param op The operator
	 * @return The constructor
	 */
	public BinaryOperator<E> getConstructor(String op) {
		return constructors.get(op);
	}
	
	/**
	 * Parses the right-side
	 * 
	 * @param tl the list of token to parse the right-side from
	 * @param op The operator
	 * @return The syntax tree object of the right side
	 */
	public E parseRhs(TokenList tl, String op) {
		return getRhsParser(op).apply(tl);
	}
	
	/**
	 * Returns the RHS parser of the operator or the default RHS parser if the supplied operator is null
	 * 
	 * @param op The operator or null
	 * @return The parser
	 */
	public Function<TokenList, E> getRhsParser(String op) {
		if (op == null)
			return defaultRhsParser;
		return rhsParsers.get(op);
	}
}
