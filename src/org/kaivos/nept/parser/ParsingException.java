package org.kaivos.nept.parser;

public class ParsingException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = -6377200708579136025L;

	String message;
	Token token;
	
	public ParsingException(String message, Token token) {
		this.message = message;
		this.token = token;
	}
	
	@Override
	public String getMessage() {
		return "Syntax error on token `" + token.getToken() + "' in " + token.getFile() + ":" + token.getLine() + ": " + message;
	}
	
}
