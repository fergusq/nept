package org.kaivos.nept.parser;

import java.util.HashMap;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * A database for operators
 * 
 * @author Iikka Hauhio
 *
 * @param <E> The type of the primary tree object
 */
public class OperatorLibrary<E> {

	private HashMap<String, BiFunction<E, E, E>> constructors = new HashMap<>();
	private HashMap<String, Integer> precedence = new HashMap<>();
	private HashMap<String, Supplier<E>> rhsParsers = new HashMap<>();
	private Supplier<E> defaultRhsParser;
	private int level = 0;
	
	public OperatorLibrary(Supplier<E> defaultRhsParser) {
		this.defaultRhsParser = defaultRhsParser;
	}
	
	/**
	 * Adds a new operator to the current precedence level
	 * 
	 * @param op The operator
	 * @param rhsParser The right-side parser function
	 * @param handler The constructor function
	 */
	public void add(String op, BiFunction<E, E, E> handler) {
		add(op, level, handler);
	}
	
	/**
	 * Adds a new operator to the current precedence level
	 * 
	 * @param op The operator
	 * @param handler The constructor function
	 */
	public void add(String op, Supplier<E> rhsParser, BiFunction<E, E, E> handler) {
		add(op, level, rhsParser, handler);
	}
	
	/**
	 * Adds a new operator
	 * 
	 * @param op The operator
	 * @param level The precedence level
	 * @param handler The constructor function
	 */
	public void add(String op, int level, BiFunction<E, E, E> handler) {
		add(op, level, defaultRhsParser, handler);
	}
	
	/**
	 * Adds a new operator
	 * 
	 * @param op The operator
	 * @param level The precedence level
	 * @param rhsParser The right-side parser function
	 * @param handler The constructor function
	 */
	public void add(String op, int level, Supplier<E> rhsParser, BiFunction<E, E, E> handler) {
		constructors.put(op, handler);
		precedence.put(op, level);
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
	 * @param a
	 * @param b
	 * @return The return value of the constructor function
	 */
	public E construct(String op, E a, E b) {
		return constructors.get(op).apply(a, b);
	}
	
	public E parseRhs(String op) {
		return rhsParsers.get(op).get();
	}
}
