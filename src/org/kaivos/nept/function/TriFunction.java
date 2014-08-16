package org.kaivos.nept.function;

@FunctionalInterface
public interface TriFunction<T, U, V, R> {
	public R apply(T a, U b, V c);
}