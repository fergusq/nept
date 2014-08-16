package org.kaivos.nept.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Constructs a parser
 * 
 * @author Iikka Hauhio
 *
 */
public class ParserConstructor {
	
	public interface ParsingStep {
		public void parse(TokenList tl) throws ParsingException;
	}
	
	private ArrayList<ParsingStep> steps = new ArrayList<>();
	
	/**
	 * Creates a new parser constructor
	 * 
	 * @return a new pc
	 */
	public static ParserConstructor npc() {
		return new ParserConstructor();
	}
	
	public ParserConstructor() {}
	
	/**
	 * Returns the constructed parser
	 * 
	 * @return the new parser
	 */
	public ParsingStep getParser() {
		return (tl) -> {
			for (ParsingStep step : steps) {
				step.parse(tl);
			}
		};
	}
	
	/**
	 * 
	 * @param keyword
	 * @return self
	 */
	public ParserConstructor ACCEPT(String... keyword) {
		steps.add(tl -> {
			Token next = tl.next();
			if (!Arrays.asList(keyword).contains(next.getToken()))
				throw new ParsingException("Expected `" + keyword + "'", next);
		});
		return this;
	}
	
	/**
	 * 
	 * @param keyword
	 * @param sub
	 * @return self
	 */
	public ParserConstructor IF(String keyword, ParsingStep sub) {
		IF(new String[] { keyword }, sub);
		return this;
	}
	
	/**
	 * 
	 * @param keyword
	 * @param sub
	 * @return self
	 */
	public ParserConstructor IF(String[] keyword, ParsingStep sub) {
		steps.add(tl -> {
			Token next = tl.seek();
			if (Arrays.asList(keyword).contains(next.getToken()))
				sub.parse(tl);
		});
		return this;
	}
	
	/**
	 * 
	 * @param pred
	 * @param sub
	 * @return self
	 */
	public ParserConstructor IF(Predicate<TokenList> pred, ParsingStep sub) {
		steps.add(tl -> {
			if (pred.test(tl));
				sub.parse(tl);
		});
		return this;
	}
	
	/**
	 * 
	 * @param keyword
	 * @param sub
	 * @return self
	 */
	public ParserConstructor WHILE(String keyword, ParsingStep sub) {
		WHILE(new String[] { keyword }, sub);
		return this;
	}
	
	/**
	 * 
	 * @param keyword
	 * @param sub
	 * @return self
	 */
	public ParserConstructor WHILE(String[] keyword, ParsingStep sub) {
		steps.add(tl -> {
			Token next = tl.seek();
			while (Arrays.asList(keyword).contains(next.getToken()))
				sub.parse(tl);
		});
		return this;
	}
	
	/**
	 * 
	 * @param pred
	 * @param sub
	 * @return self
	 */
	public ParserConstructor WHILE(Predicate<TokenList> pred, ParsingStep sub) {
		steps.add(tl -> {
			while (pred.test(tl));
				sub.parse(tl);
		});
		return this;
	}
	
	/**
	 * 
	 * @param consumer
	 * @return self
	 */
	public ParserConstructor CONSUME(Consumer<String> consumer) {
		steps.add((tl) -> {
			consumer.accept(tl.next().getToken());
		});
		return this;
	}
	
	/**
	 * Adds sub to the step list
	 * @param sub
	 * @return self
	 */
	public ParserConstructor SUB(ParsingStep sub) {
		steps.add(sub);
		return this;
	}
	
}
