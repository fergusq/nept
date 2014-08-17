package org.kaivos.nept.test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.kaivos.nept.parser.OperatorLibrary;
import org.kaivos.nept.parser.OperatorPrecedenceParser;
import org.kaivos.nept.parser.TokenList;
import org.kaivos.nept.parser.TokenScanner;

public class TokenScannerTester {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		TokenScanner t = new TokenScanner()
			.addOperators("+-*/%()[]")
			.separateIdentifiersAndPunctuation(false)
			.appendOnEOF("<EOF>");
		
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String line = "";
		while ((line = in.readLine()) != null) {
			Calculator c = new Calculator(t.tokenize(line, "<stdin>"));
			System.out.println(c.parse());
		}
	}
	
	public static class Calculator {
	
		private OperatorLibrary<Integer> library;
		private OperatorPrecedenceParser<Integer> opparser;
		private TokenList tokenlist;
		
		public Calculator(TokenList tl) {
			this.tokenlist = tl;
			
			library = new OperatorLibrary<>(() -> parsePrimary(tl));
			library.add("+", (a, b) -> a + b);
			library.add("-", (a, b) -> a - b);
			library.increaseLevel();
			library.add("*", (a, b) -> a * b);
			library.add("/", (a, b) -> a / b);
			
			opparser = new OperatorPrecedenceParser<>(library);
		}
		
		public int parse() {
			return parseExpression(tokenlist);
		}
		
		private int parseExpression(TokenList tl) {
			return opparser.parse(tl, parsePrimary(tl));
		}
		
		private int parsePrimary(TokenList tl) {
			if (tl.isNext("-"))
				return -parsePrimary(tl);
			if (tl.isNext("(")) {
				tl.accept("(");
				int i = parseExpression(tl);
				tl.accept(")");
				return i;
			}
			else return Integer.parseInt(tl.nextString());
		}
	
	}

}
