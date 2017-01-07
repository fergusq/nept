package org.kaivos.nept.parser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads tokens from a string
 * 
 * @author Iikka Hauhio
 *
 */
public class TokenScanner {

	private static class Pair<T, U> {
		private final T a;
		private final U b;

		public Pair(T a, U b) {
			super();
			this.a = a;
			this.b = b;
		}

		public T getA() {
			return a;
		}

		public U getB() {
			return b;
		}
	}

	private static class Trair<T, U, V> {
		private final T a;
		private final U b;
		private final V c;

		public Trair(T a, U b, V c) {
			super();
			this.a = a;
			this.b = b;
			this.c = c;
		}

		public T getA() {
			return a;
		}

		public U getB() {
			return b;
		}

		public V getC() {
			return c;
		}
	}

	private ArrayList<String> ignore = new ArrayList<>();
	private ArrayList<Character> dontIgnore = new ArrayList<>();
	private ArrayList<Pair<String, String>> ignoreBlocks = new ArrayList<>();
	private boolean ignoreWhitespace = true;

	private ArrayList<Pair<char[], Pattern>> patterns = new ArrayList<>();
	private ArrayList<String> operators = new ArrayList<>();
	private ArrayList<Trair<String, String, Character>> stringBlocks = new ArrayList<>();
	private ArrayList<Pair<Character, String>> escapeCodes = new ArrayList<>();
	private ArrayList<Trair<Character, Integer, Integer>> charEscapeCodes = new ArrayList<>();
	private boolean allPunctuation = true;

	private String EOF = null;

	/**
	 * Tells the scanner to ignore all instances of this character in the source code
	 * 
	 * @param chr The character
	 * @return self
	 */
	public TokenScanner ignore(char chr) {
		ignore(""+chr);
		return this;
	}

	/**
	 * Tells the scanner to ignore all instances of this sequence in the source code
	 * 
	 * @param seq The sequence
	 * @return self
	 */
	public TokenScanner ignore(String seq) {
		if (seq.length() == 1 && dontIgnore.contains(seq.charAt(0)))
			throw new IllegalArgumentException("The character being ignored is already marked not to be ignored");

		ignore.add(seq);
		return this;
	}

	/**
	 * Tells the scanner to ignore all instances of this character in the source code
	 * 
	 * @param chr The character
	 * @return self
	 */
	public TokenScanner dontIgnore(char chr) {
		if (ignore.contains(""+chr))
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
	 * @param startsWith Possible characters that can start the pattern for optimization purposes. Can be left empty.
	 * @return self
	 */
	public TokenScanner addPatternRule(Pattern p, char... startsWith) {
		Arrays.sort(startsWith);
		patterns.add(new Pair<>(startsWith, p));
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
		stringBlocks.add(new Trair<>(""+start, ""+end, escape));
		return this;
	}

	/**
	 * Decalres a new string rule
	 * 
	 * @param start The start sequence
	 * @param end The end sequence
	 * @param escape The escape character
	 * @return self
	 */
	public TokenScanner addStringRule(String start, String end, char escape) {
		stringBlocks.add(new Trair<>(start, end, escape));
		return this;
	}

	/**
	 * Declares a new escape code rule to be used in string literals
	 *
	 * @param code The character used as the first part of the escape code
	 * @param replacement The character to which the escape code expands
	 * @return self
	 */
	public TokenScanner addEscapeCode(char code, String replacement) {
		escapeCodes.add(new Pair<>(code, replacement));
		return this;
	}

	/**
	 * Declares a new escape code rule to be used in string literals
	 *
	 * A character escape code is of form \xNNNN. You can specify the
	 * start character x, the number of digits and the radix
	 *
	 * @param code The character used as the first part of the escape code
	 * @param numberOfDigits The number of digits after the first character
	 * @param radix The radix of the number
	 * @return self
	 */
	public TokenScanner addCharacterEscapeCode(char code, int numberOfDigits, int radix) {
		charEscapeCodes.add(new Trair<>(code, numberOfDigits, radix));
		return this;
	}

	/**
	 * Declares a new comment rule, tells the scanner to ignore all characters between start and end tokens
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
	 * Returns the operator list
	 * 
	 * @return see above
	 */
	public List<String> getOperators() {
		return operators;
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
		return tokenize(source, file, 1);
	}

	/**
	 * Reads tokens
	 * 
	 * @param source The string
	 * @param file The name of the file or stream
	 * @param firstLine The line number of source in the original file
	 * @return A TokenList
	 */
	public TokenList tokenize(String source, String file, int firstLine) {
		ArrayList<Token> tokens = new ArrayList<Token>();

		int line = firstLine;
		String currToken = "";

		int i = -1;
		outer: while (i < source.length()-1) {
			i++;

			if (source.charAt(i)=='\n') line++;
			if (!dontIgnore.contains(source.charAt(i)) && ignoreWhitespace && Character.isWhitespace(source.charAt(i))) {
				if (!currToken.isEmpty()) tokens.add(new Token(currToken, file, line));
				currToken = "";
				continue;
			}

			String future = source.substring(i);

			for (String seq : ignore) {
				if (future.startsWith(seq)) {
					if (!currToken.isEmpty()) tokens.add(new Token(currToken, file, line));
					currToken = "";
					i += seq.length()-1;
					continue outer;
				}
			}

			for (Pair<String, String> block : ignoreBlocks) {
				String startSeq = block.getA();
				String endSeq = block.getB();
				if (future.startsWith(startSeq)) {
					if (!currToken.isEmpty()) tokens.add(new Token(currToken, file, line));
					currToken = "";

					i += startSeq.length()-1;
					
					while (true) {
						i++;
						System.err.println(source.substring(i));
						if (source.startsWith(endSeq, i)) {
							i += endSeq.length()-1;
							continue outer;
						} else {
							if (source.length() <= i)
								throw new ParsingException("Unexpected EOF in the middle of a comment", new Token(EOF, file, line));
							if (source.charAt(i)=='\n')
								line++;
						}
					}
				}
			}
			for (Pair<char[], Pattern> p : patterns) {
				if (Arrays.binarySearch(p.a, future.charAt(0)) >= 0) {
					Matcher m = p.b.matcher(future);
					if (m.find() && m.start() == 0) {
						if (!currToken.isEmpty()) tokens.add(new Token(currToken, file, line)); currToken = "";
						tokens.add(new Token(source.substring(i, i+m.end()), file, line));
						i = i+m.end()-1;
						continue outer;
					}
				}
			}

			for (Trair<String, String, Character> block : stringBlocks) {
				String startSeq = block.getA();
				String endSeq = block.getB();
				char escapeChar = block.getC();
				if (future.startsWith(startSeq)) {
					if (!currToken.isEmpty()) tokens.add(new Token(currToken, file, line));
					currToken = "";

					StringBuilder str = new StringBuilder();
					tokens.add(new Token(startSeq, file, line));

					i += startSeq.length()-1;

					stringLoop: while (true) {
						i++;
						
						if (source.length() > i && source.charAt(i) == '\n') line++;

						if (escapeChar != '\0' && source.length() > i && source.charAt(i) == escapeChar) {
							if (source.startsWith(endSeq, i+1)) {
								str.append(endSeq);
								i += endSeq.length();
								continue;
							}
							else if (source.length() >= i+2) {
								for (Pair<Character, String> escapeCode : escapeCodes) {
									if (source.charAt(i+1) == escapeCode.getA()) {
										str.append(escapeCode.getB());
										i++;
										continue stringLoop;
									}
								}
								for (Trair<Character, Integer, Integer> escapeCode : charEscapeCodes) {
									if (source.charAt(i+1) == escapeCode.getA()) {
										String characterCode = "";
										for (int j = 2; j < escapeCode.getB()+2; j++) {
											characterCode += source.charAt(i+2);
											i++;
										}
										str.append((char) Integer.parseInt(characterCode, escapeCode.getC()));
										i++;
										continue stringLoop;
									}
								}
								throw new ParsingException("Invalid escape sequence '" + escapeChar + source.charAt(i+1) + "'", new Token(block.getA()+str, file, line));
							}
						}
						if (source.startsWith(endSeq, i)) {
							tokens.add(new Token(str.toString(), file, line));
							tokens.add(new Token(endSeq, file, line));
							i += endSeq.length()-1;
							continue outer;
						} else if (source.length() > i) {
							str.append(source.charAt(i));
						} else {
							throw new ParsingException("Unexpected EOF in the middle of a string constant", new Token(EOF, file, line));
						}
					}
				}
			}

			for (String op : operators) {
				if (future.startsWith(op)) {
					if (!currToken.isEmpty()) tokens.add(new Token(currToken, file, line)); currToken = "";
					tokens.add(new Token(op, file, line));
					i = i + op.length()-1;
					continue outer;
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
