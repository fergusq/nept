package org.kaivos.nept.parser.constructor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.kaivos.nept.parser.Parser;
import org.kaivos.nept.parser.ParsingException;
import org.kaivos.nept.parser.Token;
import org.kaivos.nept.parser.TokenList;

/**
 * Constructs a parser TODO UNDER CONSTRUCTION
 * 
 * @author Iikka Hauhio
 *
 */
public class ParserConstructor {
	
	private HashMap<String, PCParser> nodes = new HashMap<>();
	
	public class PCParser implements Parser<Void>, ParsingStep {	
		private List<ParsingStep> steps = new ArrayList<>();
		private Set<String> startTokens;
		private boolean start = false;
		
		private PCParser or = null;
		
		private HashMap<String, ArrayList<Object>> properties = new HashMap<>();
		
		public PCParser() {
			this.steps = new ArrayList<>();
		}
		
		private PCParser(List<ParsingStep> steps) {
			this.steps = steps;
		}
		
		@Override
		public Void parse(TokenList tl) throws ParsingException {
			if (or != null) {
				if (!startTokens.contains(tl.seekString()) || or.startTokens.contains(tl.seekString())) {
					or.parse(tl);
					return null;
				}
			}
			for (ParsingStep step : steps) {
				step.parseStep(tl);
			}
			return null;
		}
		
		@Override
		public void parseStep(TokenList tl) throws ParsingException {
			parse(tl);
		}
		
		private Set<String> getStartTokens() {
			Set<String> news = new HashSet<>();
			news.addAll(startTokens);
			if (or != null) news.addAll(or.startTokens);
			return news;
		}
		
		protected void setProperty(String name, Object value) {
			if (properties.get(name) == null)
				properties.put(name, new ArrayList<>());
			properties.get(name).add(value);
		}
		
		/**
		 * 
		 * @param keyword
		 * @return self
		 */
		public PCParser ACCEPT(String... keyword) {
			if (start) startTokens.addAll(Arrays.asList(keyword));
			steps.add(tl -> {
				Token next = tl.next();
				if (!Arrays.asList(keyword).contains(next.getToken()))
					throw new ParsingException("Expected one of `"
							+ Arrays.asList(keyword).stream().collect(Collectors.joining("', `")) + "'", next);
			});
			start = true;
			return this;
		}
		
		/**
		 * 
		 * @param keyword
		 * @return self
		 */
		public PCParser PACCEPT(String name, String... keyword) {
			if (start) startTokens.addAll(Arrays.asList(keyword));
			steps.add(tl -> {
				Token next = tl.next();
				if (!Arrays.asList(keyword).contains(next.getToken()))
					throw new ParsingException("Expected one of `"
							+ Arrays.asList(keyword).stream().collect(Collectors.joining("', `")) + "'", next);
				setProperty(name, next.getToken());
			});
			start = true;
			return this;
		}
		
		/**
		 * Optional node
		 * 
		 * @param sub
		 * @return
		 */
		public PCParser MAYBE(PCParser sub) {
			if (start) startTokens.addAll(sub.getStartTokens());
			steps.add(tl -> {
				Token next = tl.seek();
				if (sub.getStartTokens().contains(next.getToken()))
					sub.parse(tl);
			});
			return this;
		}
		
		/**
		 * Optional many times
		 * 
		 * @param sub
		 * @return
		 */
		public PCParser MANY(PCParser sub) {
			if (start) startTokens.addAll(sub.getStartTokens());
			steps.add(tl -> {
				Token next = tl.seek();
				while (sub.getStartTokens().contains(next.getToken()))
					sub.parse(tl);
			});
			return this;
		}
		
		/**
		 * Required once or more
		 * 
		 * @param sub
		 * @return
		 */
		public PCParser ONCEORMORE(PCParser sub) {
			if (start) startTokens.addAll(sub.getStartTokens());
			steps.add(tl -> {
				Token next = tl.seek();
				if (!sub.getStartTokens().contains(next.getToken()))
					throw new ParsingException("Expected one of `"
							+ sub.getStartTokens().stream().collect(Collectors.joining("', `")) + "'", next);
				while (sub.getStartTokens().contains(next.getToken()))
					sub.parse(tl);
			});
			return this;
		}
		
		/**
		 * 
		 * @return self
		 */
		public PCParser OR() {
			final PCParser parser = this;
			return this.or = ParserConstructor.this.new PCParser(new ArrayList<>()) {
				@Override
				protected void setProperty(String name, Object value) {
					parser.setProperty(name, value);
				}
			};
		}
		
		/**
		 * Adds node to the step list
		 * @param sub
		 * @return self
		 */
		public PCParser NODE(String name) {
			steps.add(tl -> {
				getParser(name).parse(tl);
			});
			return this;
		}
		
		/**
		 * Adds node to the step list
		 * @param sub
		 * @return self
		 */
		public PCParser NODE(String propertyName, String nodeName) {
			steps.add(tl -> {
				if (properties.get(propertyName) == null)
					properties.put(propertyName, new ArrayList<>());
				properties.get(propertyName).add(getParser(nodeName).parse(tl));
			});
			return this;
		}
		
		/**
		 * 
		 * @param keyword
		 * @param sub
		 * @return self
		 */
		public PCParser IF(String keyword, ParsingStep sub) {
			IF(new String[] { keyword }, sub);
			return this;
		}
		
		/**
		 * 
		 * @param keyword
		 * @param sub
		 * @return self
		 */
		public PCParser IF(String[] keyword, ParsingStep sub) {
			steps.add(tl -> {
				Token next = tl.seek();
				if (Arrays.asList(keyword).contains(next.getToken()))
					sub.parseStep(tl);
			});
			return this;
		}
		
		/**
		 * 
		 * @param pred
		 * @param sub
		 * @return self
		 */
		public PCParser IF(Predicate<TokenList> pred, ParsingStep sub) {
			steps.add(tl -> {
				if (pred.test(tl));
					sub.parseStep(tl);
			});
			return this;
		}
		
		/**
		 * 
		 * @param keyword
		 * @param sub
		 * @return self
		 */
		public PCParser WHILE(String keyword, ParsingStep sub) {
			WHILE(new String[] { keyword }, sub);
			return this;
		}
		
		/**
		 * 
		 * @param keyword
		 * @param sub
		 * @return self
		 */
		public PCParser WHILE(String[] keyword, ParsingStep sub) {
			steps.add(tl -> {
				Token next = tl.seek();
				while (Arrays.asList(keyword).contains(next.getToken()))
					sub.parseStep(tl);
			});
			return this;
		}
		
		/**
		 * 
		 * @param pred
		 * @param sub
		 * @return self
		 */
		public PCParser WHILE(Predicate<TokenList> pred, ParsingStep sub) {
			steps.add(tl -> {
				while (pred.test(tl));
					sub.parseStep(tl);
			});
			return this;
		}
		
		/**
		 * 
		 * @param consumer
		 * @return self
		 */
		public PCParser CONSUME(Consumer<String> consumer) {
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
		public PCParser SUB(ParsingStep sub) {
			steps.add(sub);
			return this;
		}
	}
	
	public interface ParsingStep {
		public void parseStep(TokenList tl) throws ParsingException;
	}
	
	
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
	public Parser<Void> getParser(String name) {
		return nodes.get(name);
	}
	
	public PCParser node(String name) {
		PCParser p;
		nodes.put(name, p = new PCParser(new ArrayList<>()));
		return p;
	}
	
}
