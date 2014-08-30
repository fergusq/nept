package org.kaivos.nept.parser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads tokens from a string
 * 
 * @author Iikka Hauhio
 *
 */
public class TokenScanner {
	
	private static class Pair<T> {
		private final T a;
		private final T b;
		
		public Pair(T a, T b) {
			super();
			this.a = a;
			this.b = b;
		}

		public T getA() {
			return a;
		}

		public T getB() {
			return b;
		}
	}
	
	private static class Trair<T> {
		private final T a;
		private final T b;
		private final T c;
		
		public Trair(T a, T b, T c) {
			super();
			this.a = a;
			this.b = b;
			this.c = c;
		}

		public T getA() {
			return a;
		}

		public T getB() {
			return b;
		}
		
		public T getC() {
			return c;
		}
	}
	
	private ArrayList<Character> ignore = new ArrayList<>();
	private ArrayList<Character> dontIgnore = new ArrayList<>();
	private ArrayList<Pair<String>> ignoreBlocks = new ArrayList<>();
	private boolean ignoreWhitespace = true;
	
	private ArrayList<Pattern> patterns = new ArrayList<>();
	private ArrayList<String> operators = new ArrayList<>();
	private ArrayList<Trair<Character>> stringBlocks = new ArrayList<>();
	private boolean allPunctuation = true;
	
	private String EOF = null;
	
	/**
	 * Tells the scanner to ignore all instances of this character in the source code
	 * 
	 * @param chr The character
	 * @return self
	 */
	public TokenScanner ignore(char chr) {
		if (dontIgnore.contains(chr))
			throw new IllegalArgumentException("The character being ignored is already marked not to be ignored");
		
		ignore.add(chr);
		return this;
	}
	
	/**
	 * Tells the scanner to ignore all instances of this character in the source code
	 * 
	 * @param chr The character
	 * @return self
	 */
	public TokenScanner dontIgnore(char chr) {
		if (ignore.contains(chr))
			throw new IllegalArgumentException("The character marked to not being ignored is already marked to be ignored");
		
		dontIgnore.add(chr);
		return this;
	}
	
	/**
	 * Tells the scanner to ignore all whitespace characters identified by the {@link Character}.isWhitespace. All whitespace characters are ignored by default.
	 * 
	 * @param value true if whitespace is ignored, otherwise false
	 * @return self
	 */
	public TokenScanner ignoreWhitespace(boolean value) {
		ignoreWhitespace = value;
		return this;
	}
	
	/**
	 * Tells the scanner to append a string to the end of the list when EOF is encountered.
	 * 
	 * @param text The text to be appended
	 * @return self
	 */
	public TokenScanner appendOnEOF(String text) {
		EOF = text;
		return this;
	}
	
	/**
	 * Declares a new pattern rule
	 * 
	 * @param p The pattern
	 * @return self
	 */
	public TokenScanner addPatternRule(Pattern p) {
		patterns.add(p);
		return this;
	}
	
	/**
	 * Declares a new operator rule
	 * 
	 * @param operator The operator
	 * @return self
	 */
	public TokenScanner addOperatorRule(String operator) {
		operators.add(operator);
		return this;
	}
	
	/**
	 * Decalres a new string rule
	 * 
	 * @param start The start character
	 * @param end The end character
	 * @param escape The escape character
	 * @return self
	 */
	public TokenScanner addStringRule(char start, char end, char escape) {
		stringBlocks.add(new Trair<>(start, end, escape));
		return this;
	}
	
	/**
	 * Decalres a new comment rule, tells the scanner to ignore all characters between start and end tokens
	 * 
	 * @param start The start token
	 * @param end The end token
	 * @return self
	 */
	public TokenScanner addCommentRule(String start, String end) {
		ignoreBlocks.add(new Pair<>(start, end));
		return this;
	}
	
	/**
	 * Declares all characters of the parameter string as one character operators
	 * 
	 * @param operatorString The operators
	 * @return self
	 */
	public TokenScanner addOperators(String operatorString) {
		for (char chr : operatorString.toCharArray())
			addOperatorRule(""+chr);
		return this;
	}
	
	/**
	 * Tells scanner to separate all non-letter and non-digit characters from letter and digit characters. This is set to true by default.
	 * 
	 * @param value true or false
	 * @return self
	 */
	public TokenScanner separateIdentifiersAndPunctuation(boolean value) {
		allPunctuation = value;
		return this;
	}
	
	/**
	 * Reads tokens from a file
	 * 
	 * @param file The file
	 * @return A TokenList
	 * @throws IOException on io error
	 */
	public TokenList tokenize(File file) throws IOException {
		String content = "";
		
		for (String s : java.nio.file.Files.readAllLines(Paths.get(file.toURI()), Charset.defaultCharset())) {
			content += s + "\n";
		}
		
		return tokenize(content, file.getName());
	}
	
	/**
	 * Reads tokens
	 * 
	 * @param source The string
	 * @param file The name of the file or stream
	 * @return A TokenList
	 */
	public TokenList tokenize(String source, String file) {
		ArrayList<Token> tokens = new ArrayList<Token>();
		
		int line = 0;
		String currToken = "";
		
		int i = -1;
		outer: while (i < source.length()-1) {
			i++;
			
			if (source.charAt(i)=='\n') line++;
			if (ignore.contains(source.charAt(i))) {
				if (!currToken.isEmpty()) tokens.add(new Token(currToken, file, line)); currToken = "";
				continue;
			}
			if (!dontIgnore.contains(source.charAt(i)) && ignoreWhitespace && Character.isWhitespace(source.charAt(i))) {
				if (!currToken.isEmpty()) tokens.add(new Token(currToken, file, line)); currToken = "";
				continue;
			}
			
			String future = source.substring(i);
			
			for (Pair<String> block : ignoreBlocks) {
				String op = block.getA();
				if (future.length() >= op.length() && future.substring(0, op.length()).equals(op)) {
					if (!currToken.isEmpty()) tokens.add(new Token(currToken, file, line)); currToken = "";
					
					op = block.getB();
					while (true) if (future.length() >= op.length() && future.substring(0, op.length()).equals(op)) {
						i = i + op.length()-1;
						continue outer;
					} else {
						future = source.substring(++i);
						if (source.length() > i && source.charAt(i)=='\n') line++;
					}
				}
			}
			
			for (String op : operators) {
				if (future.length() >= op.length() && future.substring(0, op.length()).equals(op)) {
					if (!currToken.isEmpty()) tokens.add(new Token(currToken, file, line)); currToken = "";
					tokens.add(new Token(op, file, line));
					i = i + op.length()-1;
					continue outer;
				}
			}
			for (Pattern p : patterns) {
				Matcher m = p.matcher(future);
				if (m.find() && m.start() == 0) {
					if (!currToken.isEmpty()) tokens.add(new Token(currToken, file, line)); currToken = "";
					tokens.add(new Token(source.substring(i, i+m.end()), file, line));
					i = i+m.end()-1;
					continue outer;
				}
			}
			
			for (Trair<Character> block : stringBlocks) {
				if (future.length() >= 1 && future.charAt(0) == block.getA()) {
					if (!currToken.isEmpty()) tokens.add(new Token(currToken, file, line)); currToken = "";
					
					String str = ""+block.getA();
					
					while (true) {
						future = source.substring(++i);
						if (source.length() > i && source.charAt(i)=='\n') line++;
						
						if (future.length() >= 1 && future.charAt(0) == block.getC()) {
							if (future.length() >= 2 && future.charAt(1) == block.getB()) {
								str += block.getB();
								i++;
								continue;
							};
						}
						if (future.length() >= 1 && future.charAt(0) == block.getB()) {
							str += block.getB();
							tokens.add(new Token(str, file, line));
							continue outer;
						} else if (future.length() >= 1) {
							str += future.charAt(0);
						} else {
							throw new ParsingException("Unexpected EOF in the middle of string constant", new Token(EOF, file, line));
						}
					}
				}
			}
			
			if (allPunctuation && !Character.isLetter(future.charAt(0)) && !Character.isDigit(future.charAt(0))) {
				if (!currToken.isEmpty()) tokens.add(new Token(currToken, file, line)); currToken = "";
				tokens.add(new Token(""+future.charAt(0), file, line));
				i = i + 0;
				continue outer;
			}
		
			currToken += future.charAt(0);
		}
		
		if (!currToken.isEmpty())
			tokens.add(new Token(currToken, file, line));
		
		if (EOF != null && !EOF.isEmpty())
			tokens.add(new Token(EOF, file, line));
		
		return new TokenList(tokens);
	}
	
}
