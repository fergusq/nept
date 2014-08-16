package org.kaivos.nept.parser;

/**
 * Represents a token
 * 
 * @author Iikka Hauhio
 *
 */
public class Token {
	
	private String token;
	private String file;
	private int line;
	
	Token(String token, String file, int line) {
		this.token = token;
		this.file = file;
		this.line = line;
	}
	
	/**
	 * The string representation of the token
	 * 
	 * @return A string
	 */
	public String getToken() {
		return token;
	}
	
	/**
	 * The file
	 * 
	 * @return A string
	 */
	public String getFile() {
		return file;
	}
	
	/**
	 * Location in the file
	 * 
	 * @return An integer
	 */
	public int getLine() {
		return line;
	}
	
	@Override
	public String toString() {
		return token;
	}
	
}
