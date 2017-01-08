package org.kaivos.nept.parser;

//import static java.util.stream.Collectors.toList;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
//import java.util.HashMap;
import java.util.List;
//import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//import org.kaivos.röda.Timer;

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

	@SuppressWarnings("unchecked")
	private ArrayList<String>[] ignore = new ArrayList[256];
	private String dontIgnore = "";
	@SuppressWarnings("unchecked")
	private ArrayList<Pair<String, String>>[] ignoreBlocks = new ArrayList[256];
	private boolean ignoreWhitespace = true;

	@SuppressWarnings("unchecked")
	private ArrayList<Pattern>[] patterns = new ArrayList[256];
	
	@SuppressWarnings("unchecked")
	private ArrayList<String>[] operators = new ArrayList[256];
	private String oneCharOperators = "";
	
	@SuppressWarnings("unchecked")
	private ArrayList<Trair<String, String, Character>>[] stringBlocks = new ArrayList[256];
	private ArrayList<Pair<Character, String>> escapeCodes = new ArrayList<>();
	private ArrayList<Trair<Character, Integer, Integer>> charEscapeCodes = new ArrayList<>();
	private boolean allPunctuation = true;

	private String EOF = null;
	
	{ for (int i = 0; i < operators.length; i++) operators[i] = new ArrayList<>(); }
	{ for (int i = 0; i < ignoreBlocks.length; i++) ignoreBlocks[i] = new ArrayList<>(); }
	{ for (int i = 0; i < ignore.length; i++) ignore[i] = new ArrayList<>(); }
	{ for (int i = 0; i < stringBlocks.length; i++) stringBlocks[i] = new ArrayList<>(); }
	{ for (int i = 0; i < patterns.length; i++) patterns[i] = new ArrayList<>(); }

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
		if (seq.length() == 1 && dontIgnore.indexOf(seq.charAt(0)) >= 0)
			throw new IllegalArgumentException("The character being ignored is already marked not to be ignored");

		ignore[seq.charAt(0)].add(seq);
		return this;
	}

	/**
	 * Tells the scanner to ignore all instances of this character in the source code
	 * 
	 * @param chr The character
	 * @return self
	 */
	public TokenScanner dontIgnore(char chr) {
		if (ignore[chr].contains(""+chr))
			throw new IllegalArgumentException("The character marked to not being ignored is already marked to be ignored");

		dontIgnore += chr;
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
		for (char sw : startsWith)
			patterns[sw].add(p);
		return this;
	}

	/**
	 * Declares a new operator rule
	 * 
	 * @param operator The operator
	 * @return self
	 */
	public TokenScanner addOperatorRule(String operator) {
		operators[operator.charAt(0)].add(operator);
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
		stringBlocks[start].add(new Trair<>(""+start, ""+end, escape));
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
		stringBlocks[start.charAt(0)].add(new Trair<>(start, end, escape));
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
		ignoreBlocks[start.charAt(0)].add(new Pair<>(start, end));
		return this;
	}

	/**
	 * Declares all characters of the parameter string as one character operators
	 * 
	 * @param operatorString The operators
	 * @return self
	 */
	public TokenScanner addOperators(String operatorString) {
		oneCharOperators += operatorString;
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
		List<String> newOperators = new ArrayList<>();
		for (ArrayList<String> operatorList : operators)
			newOperators.addAll(operatorList);
		for (char chr : oneCharOperators.toCharArray())
			newOperators.add(String.valueOf(chr));
		return newOperators;
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
	
	/*
	private Timer ignoreCharsTime = new Timer(),
			ignoreTokensTime = new Timer(),
			ignoreBlocksTime = new Timer(),
			patternTime = new Timer(),
			stringBlocksTime = new Timer(),
			operatorsTime = new Timer(),
			operatorCharsTime = new Timer(),
			allPunctuationTime = new Timer(),
			substringTime = new Timer();
	
	public void printStatistics() {
		HashMap<String, Long> map = new HashMap<>();
		map.put("ignoreChars", ignoreCharsTime.timeNanos());
		map.put("ignoreTokens", ignoreTokensTime.timeNanos());
		map.put("ignoreBlocks", ignoreBlocksTime.timeNanos());
		map.put("pattern", patternTime.timeNanos());
		map.put("stringBlocks", stringBlocksTime.timeNanos());
		map.put("operators", operatorsTime.timeNanos());
		map.put("operatorChars", operatorCharsTime.timeNanos());
		map.put("allPunctuation", allPunctuationTime.timeNanos());
		map.put("substring", substringTime.timeNanos());
		
		List<Entry<String, Long>> data = map.entrySet()
				.stream().sorted((a, b) -> Long.compare(b.getValue(), a.getValue())).collect(toList());
		
		long sum = data.parallelStream().mapToLong(e -> e.getValue().longValue()).sum();
		double acc = 0;
		
		System.out.printf("%5s %5s %6s %s\n", "%", "ACC", "MS", "FUNCTION");
		
		for (Entry<String, Long> e : data) {
			String f = e.getKey();
			double time = e.getValue() / 1_000_000d;
			double percent = 100d * e.getValue() / sum;
			acc += percent;
			
			System.out.printf("%5.2f %5.2f %6.2f %s\n", percent, acc, time, f);
		}
	}
	*/

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
		StringBuilder currToken = new StringBuilder();

		char[] oneCharOperatorsArr = oneCharOperators.toCharArray();
		Arrays.sort(oneCharOperatorsArr);
		
		char[] dontIgnoreArr = dontIgnore.toCharArray();
		Arrays.sort(dontIgnoreArr);
		
		int i = -1;
		outer: while (i < source.length()-1) {
			i++;
			
			//ignoreCharsTime.start();

			if (source.charAt(i)=='\n') line++;
			if (ignoreWhitespace
					&& Character.isWhitespace(source.charAt(i))
					&& Arrays.binarySearch(dontIgnoreArr, source.charAt(i)) < 0) {
				if (currToken.length() > 0) tokens.add(new Token(currToken.toString(), file, line));
				currToken.setLength(0);
				
				//ignoreCharsTime.stop();
				continue;
			}
			
			//ignoreCharsTime.stop();

			//substringTime.start();
			
			boolean isBelow256 = source.charAt(i) < 256;

			//substringTime.stop();
			
			//ignoreTokensTime.start();
			
			if (isBelow256) {
				for (String seq : ignore[source.charAt(i)]) {
					if (source.startsWith(seq, i)) {
						if (currToken.length() > 0) tokens.add(new Token(currToken.toString(), file, line));
						currToken.setLength(0);
						i += seq.length()-1;
						
						//ignoreTokensTime.stop();
						continue outer;
					}
				}
			}
			
			//ignoreTokensTime.stop();
			
			//ignoreBlocksTime.start();

			if (isBelow256) {
				for (Pair<String, String> block : ignoreBlocks[source.charAt(i)]) {
					String startSeq = block.getA();
					String endSeq = block.getB();
					if (source.startsWith(startSeq, i)) {
						if (currToken.length() > 0) tokens.add(new Token(currToken.toString(), file, line));
						currToken.setLength(0);
	
						i += startSeq.length()-1;
						
						while (true) {
							i++;
							if (source.startsWith(endSeq, i)) {
								i += endSeq.length()-1;
								
								//ignoreBlocksTime.stop();
								continue outer;
							} else {
								if (source.length() <= i)
									throw new ParsingException("Unexpected EOF in the middle of a comment",
											new Token(EOF, file, line));
								if (source.charAt(i)=='\n')
									line++;
							}
						}
					}
				}
			}
			
			//ignoreBlocksTime.stop();
			
			//patternTime.start();
			
			if (isBelow256) {
				for (Pattern p : patterns[source.charAt(i)]) {
					Matcher m = p.matcher(source.substring(i));
					if (m.find() && m.start() == 0) {
						if (currToken.length() > 0) tokens.add(new Token(currToken.toString(), file, line));
						currToken.setLength(0);
						tokens.add(new Token(source.substring(i, i+m.end()), file, line));
						i = i+m.end()-1;
						
						//patternTime.stop();
						continue outer;
					}
				}
			}
			
			//patternTime.stop();
			
			//stringBlocksTime.start();

			if (isBelow256) {
				for (Trair<String, String, Character> block : stringBlocks[source.charAt(i)]) {
					String startSeq = block.getA();
					String endSeq = block.getB();
					char escapeChar = block.getC();
					if (source.startsWith(startSeq, i)) {
						if (currToken.length() > 0) tokens.add(new Token(currToken.toString(), file, line));
						currToken.setLength(0);
	
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
									throw new ParsingException("Invalid escape sequence '"
											+ escapeChar + source.charAt(i+1) + "'",
											new Token(block.getA()+str, file, line));
								}
							}
							if (source.startsWith(endSeq, i)) {
								tokens.add(new Token(str.toString(), file, line));
								tokens.add(new Token(endSeq, file, line));
								i += endSeq.length()-1;
								
								//stringBlocksTime.stop();
								continue outer;
							} else if (source.length() > i) {
								str.append(source.charAt(i));
							} else {
								throw new ParsingException("Unexpected EOF in the middle of a string constant",
										new Token(EOF, file, line));
							}
						}
					}
				}
			}
			
			//stringBlocksTime.stop();
			
			//operatorsTime.start();

			if (isBelow256) {
				for (String op : operators[source.charAt(i)]) {
					if (source.startsWith(op, i)) {
						if (currToken.length() > 0) tokens.add(new Token(currToken.toString(), file, line));
						currToken.setLength(0);
						tokens.add(new Token(op, file, line));
						i = i + op.length()-1;
						
						//operatorsTime.stop();
						continue outer;
					}
				}
			}
			
			//operatorsTime.stop();
			
			//operatorCharsTime.start();
			
			if (Arrays.binarySearch(oneCharOperatorsArr, source.charAt(i)) >= 0) {
				if (currToken.length() > 0) tokens.add(new Token(currToken.toString(), file, line));
				currToken.setLength(0);
				tokens.add(new Token(String.valueOf(source.charAt(i)), file, line));
				
				//operatorCharsTime.stop();
				continue outer;
			}
			
			//operatorCharsTime.stop();
			
			//allPunctuationTime.start();

			if (allPunctuation && !Character.isLetter(source.charAt(i)) && !Character.isDigit(source.charAt(i))) {
				if (currToken.length() > 0) tokens.add(new Token(currToken.toString(), file, line));
				currToken.setLength(0);
				tokens.add(new Token(""+source.charAt(i), file, line));
				i = i + 0;
				
				//allPunctuationTime.stop();
				continue outer;
			}
			
			//allPunctuationTime.stop();

			currToken.append(source.charAt(i));
		}
		
		//printStatistics();

		if (currToken.length() > 0) tokens.add(new Token(currToken.toString(), file, line));

		if (EOF != null && !EOF.isEmpty())
			tokens.add(new Token(EOF, file, line));

		return new TokenList(tokens);
	}

}
