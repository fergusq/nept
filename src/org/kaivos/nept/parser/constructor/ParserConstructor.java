package org.kaivos.nept.parser.constructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.kaivos.nept.parser.Parser;
import org.kaivos.nept.parser.ParsingException;
import org.kaivos.nept.parser.Token;
import org.kaivos.nept.parser.TokenList;

import static org.kaivos.nept.parser.TokenList.expected;

/*
 * TODO OR(converter) ei toimi 
 * 
 */

/**
 * Constructs a parser TODO UNDER CONSTRUCTION
 * 
 * @param <T> The type of the syntax tree object
 * 
 * @author Iikka Hauhio
 *
 */
public class ParserConstructor<T> {
	
	private HashMap<String, PCParser> nodes = new HashMap<>();
	
	private static <T> void put(Map<String, List<T>> map, String name, T value) {
		if (map.get(name) == null) map.put(name, new ArrayList<>());
		map.get(name).add(value);
	}
	
	/**
	 * The type returned by ParserConstructor
	 * 
	 * @author Iikka Hauhio
	 *
	 */
	public class PCParser implements Parser<Map<String, List<T>>>, ParsingStep<T> {	
		private List<ParsingStep<T>> steps = new ArrayList<>();
		private Set<String> startTokens = new HashSet<String>();
		private boolean start = true;
		
		private PCParser or = null;
		
		private Function<Map<String, List<T>>, T> converter;
		
		/**
		 * Initializes an empty PCParser
		 * @param converter The function used to convert a property map to it's real syntax tree form
		 */
		public PCParser(Function<Map<String, List<T>>, T> converter) {
			this.steps = new ArrayList<>();
			this.converter = converter;
		}
		
		@Override
		public Map<String, List<T>> parse(TokenList tl) throws ParsingException {
			Map<String, List<T>> properties = new HashMap<>();
			parseStep(tl, properties);
			return properties;
		}
		
		@Override
		public void parseStep(TokenList tl, Map<String, List<T>> values) throws ParsingException {
			if (or != null) {
				if (!startTokens.contains(tl.seekString()) || or.startTokens.contains(tl.seekString())) {
					or.parseStep(tl, values);
					return;
				}
			}
			for (ParsingStep<T> step : steps) {
				step.parseStep(tl, values);
			}
		}
		
		/**
		 * Parser to a syntax tree
		 * 
		 * @param tl The token list
		 * @return The syntax tree
		 * @throws ParsingException on case of syntax error
		 */
		public T parseTree(TokenList tl) throws ParsingException {
			return this.converter.apply(this.parse(tl));
		}
		
		private Set<String> getStartTokens() {
			Set<String> news = new HashSet<>();
			news.addAll(startTokens);
			if (or != null) news.addAll(or.startTokens);
			return news;
		}
		
		/**
		 * Accepts a keyword
		 * 
		 * @param keyword List of acceptable keywords
		 * @return self
		 */
		public PCParser ACCEPT(String... keyword) {
			if (start) startTokens.addAll(Arrays.asList(keyword));
			steps.add((tl, values) -> {
				Token next = tl.next();
				if (!Arrays.asList(keyword).contains(next.getToken()))
					throw new ParsingException(expected(keyword), next);
			});
			start = true;
			return this;
		}
		
		/**
		 * Accepts a keyword and stores it to the properties
		 *
		 * @param name The name of the property
		 * @param stringConverter A Function converting token to it's syntax tree form
		 * @param keyword List of acceptable keywords
		 * @return self
		 */
		public PCParser PACCEPT(String name, Function<String, T> stringConverter, String... keyword) {
			if (start) startTokens.addAll(Arrays.asList(keyword));
			steps.add((tl, values) -> {
				Token next = tl.next();
				if (!Arrays.asList(keyword).contains(next.getToken()))
					throw new ParsingException(expected(keyword), next);
				put(values, name, stringConverter.apply(next.getToken()));
			});
			start = true;
			return this;
		}
		
		/**
		 * Optional node
		 * 
		 * @param sub The subnode
		 * @return self
		 */
		public PCParser MAYBE(PCParser sub) {
			if (start) startTokens.addAll(sub.getStartTokens());
			steps.add((tl, values) -> {
				Token next = tl.seek();
				if (sub.getStartTokens().contains(next.getToken())) {
					sub.parse(tl).entrySet().stream().forEach(entry -> {
						if (values.containsKey(entry.getKey())) {
							values.get(entry.getKey()).addAll(entry.getValue());
						}
						else {
							values.put(entry.getKey(), entry.getValue());
						}
				});
				}
			});
			return this;
		}
		
		/**
		 * Optional many times
		 * 
		 * @param sub The subnode
		 * @return self
		 */
		public PCParser MANY(PCParser sub) {
			if (start) startTokens.addAll(sub.getStartTokens());
			steps.add((tl, values) -> {
				while (sub.getStartTokens().contains(tl.seekString())) {
					sub.parse(tl).entrySet().stream().forEach(entry -> {
						if (values.containsKey(entry.getKey())) {
							values.get(entry.getKey()).addAll(entry.getValue());
						}
						else {
							values.put(entry.getKey(), entry.getValue());
						}
					});
				}
			});
			return this;
		}
		
		/**
		 * Required once or more
		 * 
		 * @param sub The subnode
		 * @return self
		 */
		public PCParser ONCEORMORE(PCParser sub) {
			if (start) startTokens.addAll(sub.getStartTokens());
			steps.add((tl, values) -> {
				Token next = tl.seek();
				if (!sub.getStartTokens().contains(next.getToken()))
					throw new ParsingException(expected(sub.getStartTokens().toArray(new String[0])), next);
				while (sub.getStartTokens().contains(tl.seekString())) {
					sub.parse(tl).entrySet().stream().forEach(entry -> {
						if (values.containsKey(entry.getKey())) {
							values.get(entry.getKey()).addAll(entry.getValue());
						}
						else {
							values.put(entry.getKey(), entry.getValue());
						}
					});
				}
			});
			return this;
		}
		
		/**
		 * OR Operator (both sides use same converter function)
		 * 
		 * @return self
		 */
		public PCParser OR() {
			return this.or = ParserConstructor.this.new PCParser(this.converter);
		}
		
		/**
		 * OR Operator (both sides use different converter)
		 * @param rightSideConverter the converter of the right side
		 * 
		 * @return self
		 */
		public PCParser OR(Function<Map<String, List<T>>, T> rightSideConverter) {
			return this.or = ParserConstructor.this.new PCParser(rightSideConverter);
		}
		
		/**
		 * Parses a node
		 * @param name The name of the node
		 * @return self
		 */
		public PCParser NODE(String name) {
			steps.add((tl, values) -> {
				getNode(name).parse(tl);
			});
			return this;
		}
		
		/**
		 * Parses a node and adds it to the properties
		 * 
		 * @param propertyName The name of the property
		 * @param nodeName The name of the node
		 * @return self
		 */
		public PCParser NODE(String propertyName, String nodeName) {
			steps.add((tl, values) -> {
				put(values, propertyName, getNode(nodeName).converter.apply(getNode(nodeName).parse(tl)));
			});
			return this;
		}
		
		/**
		 * Continues to the subnode if the next token is one of the given keywords
		 * 
		 * @param keyword List of acceptable keywords
		 * @param sub The subnode
		 * @return self
		 */
		public PCParser IF(String keyword, ParsingStep<T> sub) {
			IF(new String[] { keyword }, sub);
			return this;
		}
		
		/**
		 * Continues to the subnode if the next token is one of the given keywords
		 * 
		 * @param keyword List of acceptable keywords
		 * @param sub The subnode
		 * @return self
		 */
		public PCParser IF(String[] keyword, ParsingStep<T> sub) {
			steps.add((tl, values) -> {
				Token next = tl.seek();
				if (Arrays.asList(keyword).contains(next.getToken()))
					sub.parseStep(tl, values);
			});
			return this;
		}
		
		/**
		 * Continues to the subnode if the pred returns true
		 * 
		 * @param pred The predicate
		 * @param sub The subnode
		 * @return self
		 */
		public PCParser IF(Predicate<TokenList> pred, ParsingStep<T> sub) {
			steps.add((tl, values) -> {
				if (pred.test(tl));
					sub.parseStep(tl, values);
			});
			return this;
		}
		
		/**
		 * Repeats subnode as long as the next token is one of the alternatives
		 * 
		 * @param keyword List of acceptable keywords
		 * @param sub The subnode
		 * @return self
		 */
		public PCParser WHILE(String keyword, ParsingStep<T> sub) {
			WHILE(new String[] { keyword }, sub);
			return this;
		}
		
		/**
		 * Repeats subnode as long as the next token is one of the alternatives
		 * 
		 * @param keyword List of acceptable keywords
		 * @param sub The subnode
		 * @return self
		 */
		public PCParser WHILE(String[] keyword, ParsingStep<T> sub) {
			steps.add((tl, values) -> {
				while (Arrays.asList(keyword).contains(tl.seekString()))
					sub.parseStep(tl, values);
			});
			return this;
		}
		
		/**
		 * Repeats the subnode as long as the pred returns true
		 * 
		 * @param pred The predicate
		 * @param sub The subnode
		 * @return self
		 */
		public PCParser WHILE(Predicate<TokenList> pred, ParsingStep<T> sub) {
			steps.add((tl, values) -> {
				while (pred.test(tl));
					sub.parseStep(tl, values);
			});
			return this;
		}
		
		/**
		 * The next token is processed by the consumer function
		 * 
		 * @param consumer The consumer
		 * @return self
		 */
		public PCParser CONSUME(Consumer<String> consumer) {
			steps.add((tl, values) -> {
				consumer.accept(tl.next().getToken());
			});
			return this;
		}
		
		/**
		 * The next token is processed by the processor function and stored to the property map
		 * 
		 * @param name The name of the property
		 * @param processor The processor
		 * @return self
		 */
		public PCParser PROCESS(String name, Function<String, T> processor) {
			steps.add((tl, values) -> {
				put(values, name, processor.apply(tl.next().getToken()));
			});
			return this;
		}
		
		/**
		 * Adds sub to the step list
		 * 
		 * @param sub The subnode
		 * @return self
		 */
		public PCParser SUB(ParsingStep<T> sub) {
			steps.add(sub);
			return this;
		}
	}
	
	/**
	 * A parsing step
	 * 
	 * @param <T> The type of the syntax tree object
	 * 
	 * @author Iikka Hauhio
	 *
	 */
	public interface ParsingStep<T> {
		
		/**
		 * Continues to the step
		 * 
		 * @param tl The token list
		 * @param values The property map
		 * @throws ParsingException on syntax error
		 */
		public void parseStep(TokenList tl, Map<String, List<T>> values) throws ParsingException;
	}
	
	
	/**
	 * Creates a new parser constructor
	 * 
	 * @return a new pc
	 */
	public static <T> ParserConstructor<T> npc() {
		return new ParserConstructor<T>();
	}
	
	/**
	 * Returns a new ParserConstructor
	 */
	public ParserConstructor() {}
	
	/**
	 * Returns a node
	 * 
	 * @param name The name of the node
	 * @return the node
	 */
	public PCParser getNode(String name) {
		return nodes.get(name);
	}
	
	/**
	 * Adds a new node to the list of the nodes and returns it
	 * 
	 * @param name The name of the new node
	 * @param converter The converter function used to convert the property map to it's right syntax tree form
	 * @return The node
	 */
	public PCParser node(String name, Function<Map<String, List<T>>, T> converter) {
		PCParser p;
		nodes.put(name, p = new PCParser(converter));
		return p;
	}
	
	/**
	 * Returns a new PCParser object
	 * 
	 * @return the object
	 */
	public PCParser node() {
		return new PCParser(null);
	}
	
}
