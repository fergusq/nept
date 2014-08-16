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
	
	private ArrayList<Character> ignore = new ArrayList<>();
	private ArrayList<Character> dontIgnore = new ArrayList<>();
	private boolean ignoreWhitespace = true;
	
	private ArrayList<Pattern> patterns = new ArrayList<>();
	private ArrayList<String> operators = new ArrayList<>();
	private boolean allPunctuation = true;
	
	public TokenScanner() {}
	
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
	 * Tells the scanner to ignore all whitespace characters identified by the {@link Character.isWhitespace}. All whitespace characters are ignored by default.
	 * 
	 * @param value true if whitespace is ignored, otherwise false
	 * @return self
	 */
	public TokenScanner ignoreWhitespace(boolean value) {
		ignoreWhitespace = value;
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
	 * Declares all characters of the parameter string as one character operators
	 * 
	 * @param operators The operators
	 * @return self
	 */
	public TokenScanner addOperators(String operators) {
		for (char chr : operators.toCharArray())
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
	 * @throws IOException
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
			if (ignore.contains(source.charAt(i)))
				continue;
			if (!dontIgnore.contains(source.charAt(i)) && ignoreWhitespace && Character.isWhitespace(source.charAt(i)))
				continue;
			String future = source.substring(i);
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
			
			if (allPunctuation && !Character.isLetter(future.charAt(0)) && !Character.isDigit(future.charAt(0))) {
				if (!currToken.isEmpty()) tokens.add(new Token(currToken, file, line)); currToken = "";
				tokens.add(new Token(""+future.charAt(0), file, line));
				i = i + 0;
				continue outer;
			}
			
			currToken += source.charAt(i);
		}
		
		if (!currToken.isEmpty())
			tokens.add(new Token(currToken, file, line));
		
		return new TokenList(tokens);
	}
	
}
