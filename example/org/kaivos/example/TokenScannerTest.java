package org.kaivos.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.kaivos.nept.parser.TokenScanner;

/**
 * Tests the {@code TokenScanner}
 * 
 * @author Iikka Hauhio
 *
 */
public class TokenScannerTest {

	/**
	 * @param args args
	 * @throws IOException on io exception
	 */
	public static void main(String[] args) throws IOException {
		TokenScanner t = new TokenScanner()
			.addOperators("+-*/%()[]")
			.separateIdentifiersAndPunctuation(false)
			.addCommentRule("/*", "*/")
			.addStringRule('"', '"', '\\')
			.appendOnEOF("<EOF>");
		
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String line = "";
		while ((line = in.readLine()) != null) {
			System.out.println(t.tokenize(line, "<stdin>"));
		}
	}

}
