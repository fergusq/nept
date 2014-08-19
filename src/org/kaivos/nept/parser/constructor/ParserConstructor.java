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

/**
 * Constructs a parser TODO UNDER CONSTRUCTION
 * 
 * @author Iikka Hauhio
 *
 */
public class ParserConstructor {
	
	private HashMap<String, PCParser> nodes = new HashMap<>();
	
	static void put(Map<String, List<Object>> map, String name, Object value) {
		if (map.get(name) == null) map.put(name, new ArrayList<>());
		map.get(name).add(value);
	}
	
	/**
	 * The type returned by ParserConstructor
	 * 
	 * @author Iikka Hauhio
	 *
	 */
	public class PCParser implements Parser<Map<String, List<Object>>>, ParsingStep {	
		private List<ParsingStep> steps = new ArrayList<>();
		private Set<String> startTokens = new HashSet<String>();
		private boolean start = true;
		
		private PCParser or = null;
		
		/**
		 * Initializes an empty PCParser
		 */
		public PCParser() {
			this.steps = new ArrayList<>();
		}
		
		@Override
		public Map<String, List<Object>> parse(TokenList tl) throws ParsingException {
			Map<String, List<Object>> properties = new HashMap<>();
			parseStep(tl, properties);
			return properties;
		}
		
		@Override
		public void parseStep(TokenList tl, Map<String, List<Object>> values) throws ParsingException {
			if (or != null) {
				if (!startTokens.contains(tl.seekString()) || or.startTokens.contains(tl.seekString())) {
					or.parseStep(tl, values);
					return;
				}
			}
			for (ParsingStep step : steps) {
				step.parseStep(tl, values);
			}
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
		 * @param keyword List of acceptable keywords
		 * @return self
		 */
		public PCParser PACCEPT(String name, String... keyword) {
			if (start) startTokens.addAll(Arrays.asList(keyword));
			steps.add((tl, values) -> {
				Token next = tl.next();
				if (!Arrays.asList(keyword).contains(next.getToken()))
					throw new ParsingException(expected(keyword), next);
				put(values, name, next.getToken());
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
		 * OR Operator
		 * 
		 * @return self
		 */
		public PCParser OR() {
			return this.or = ParserConstructor.this.new PCParser();
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
				put(values, propertyName, getNode(nodeName).parse(tl));
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
		public PCParser IF(String keyword, ParsingStep sub) {
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
		public PCParser IF(String[] keyword, ParsingStep sub) {
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
		public PCParser IF(Predicate<TokenList> pred, ParsingStep sub) {
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
		public PCParser WHILE(String keyword, ParsingStep sub) {
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
		public PCParser WHILE(String[] keyword, ParsingStep sub) {
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
		public PCParser WHILE(Predicate<TokenList> pred, ParsingStep sub) {
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
		public PCParser PROCESS(String name, Function<String, Object> processor) {
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
		public PCParser SUB(ParsingStep sub) {
			steps.add(sub);
			return this;
		}
	}
	
	/**
	 * A parsing step
	 * 
	 * @author Iikka Hauhio
	 *
	 */
	public interface ParsingStep {
		
		/**
		 * Continues to the step
		 * 
		 * @param tl The token list
		 * @param values The property map
		 * @throws ParsingException on syntax error
		 */
		public void parseStep(TokenList tl, Map<String, List<Object>> values) throws ParsingException;
	}
	
	
	/**
	 * Creates a new parser constructor
	 * 
	 * @return a new pc
	 */
	public static ParserConstructor npc() {
		return new ParserConstructor();
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
	public Parser<Map<String, List<Object>>> getNode(String name) {
		return nodes.get(name);
	}
	
	/**
	 * Adds a new node to the list of the nodes and returns it
	 * 
	 * @param name The name of the new node
	 * @return The node
	 */
	public PCParser node(String name) {
		PCParser p;
		nodes.put(name, p = new PCParser());
		return p;
	}
	
	/**
	 * Returns a new PCParser object
	 * 
	 * @return the object
	 */
	public PCParser node() {
		return new PCParser();
	}
	
}
