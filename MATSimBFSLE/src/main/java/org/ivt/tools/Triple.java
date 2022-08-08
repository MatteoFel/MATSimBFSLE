package org.ivt.tools;

import java.io.Serializable;


/**
 * A Triple stores three values (a "triplet") and respects their order.
 * This generic class implements a commonly used data structure which is not present in
 * the current collection framework. Although it could be simulated with a List containing
 * two Objects, this implementation offers type safety and maximizes convenience for programmers.
 *
 * @author mfelder
 *
 * @param <A>
 * @param <B>
 * @param <C>
 */
public final class Triple<A, B, C> implements Serializable {
	private static final long serialVersionUID = 1L;

	public static <A, B, C> Triple<A, B, C> of(final A first, final B second, final C third) {
		return new Triple<>(first, second, third);
	}

	/**
	 * First entry of the tuple
	 */
	private final A first;
	/**
	 * Second entry of the tuple
	 */
	private final B second;
	/**
	 * Third entry of the tuple
	 */
	private final C third;
	/**
	 * Creates a new tuple with the two entries.
	 * @param first
	 * @param second
	 */
	public Triple(final A first, final B second, final C third) {
		this.first = first;
		this.second = second;
		this.third = third;
	}

	public A getFirst() {
		return this.first;
	}

	public B getSecond() {
		return this.second;
	}
	
	public C getThird() {
		return this.third;
	}
}


