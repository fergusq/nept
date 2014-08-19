package org.kaivos.nept.function;

/**
 * A function with three parameters
 * 
 * @author Iikka Hauhio
 *
 * @param <T> The type of the first parameter
 * @param <U> The type of the second parameter
 * @param <V> The type of the third parameter
 * @param <R> The return type
 */

@FunctionalInterface
public interface TriFunction<T, U, V, R> {
	
	/**
	 * Applies the function with arguments
	 * 
	 * @param a The first argument
	 * @param b The second
	 * @param c The third
	 * @return The return value
	 */
	public R apply(T a, U b, V c);
}