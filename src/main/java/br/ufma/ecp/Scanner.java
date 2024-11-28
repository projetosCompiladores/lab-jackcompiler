package br.ufma.ecp;

import static br.ufma.ecp.token.TokenType.EOF;
import static br.ufma.ecp.token.TokenType.IDENT;
import static br.ufma.ecp.token.TokenType.ILLEGAL;
import static br.ufma.ecp.token.TokenType.MINUS;
import static br.ufma.ecp.token.TokenType.NUMBER;
import static br.ufma.ecp.token.TokenType.PLUS;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import br.ufma.ecp.token.Token;
import br.ufma.ecp.token.TokenType;

public class Scanner {

    private byte[] input;
    private int current;
    private int start;

    private static final Map<String, TokenType> keywords;
 

    static {
        keywords = new HashMap<>();
        keywords.put("method",    TokenType.METHOD);
        keywords.put("while",  TokenType.WHILE);
        keywords.put("if",   TokenType.IF);
    }

    
    public Scanner (byte[] input) {
        this.input = input;
        current = 0;
        start = 0;
    }

    private void skipWhitespace() {
        char ch = peek();
        while (ch == ' ' || ch == '\r' || ch == '\t' || ch == '\n') {
            advance();
            ch = peek();
        }
    }
    

    public Token nextToken () {

        skipWhitespace();

        start = current;
        char ch = peek();

        if (Character.isDigit(ch)) {
            return number();
        }

        if (isAlpha(ch)) {
            return identifier();
        }

        switch (ch) {
            case '+':
                advance();
                return new Token (PLUS,"+");
            case '-':
                advance();
                return new Token (MINUS,"-");
            case '"':
                return string();
            case 0:
                return new Token (EOF,"EOF");
            default:
                advance();
                return new Token(ILLEGAL, Character.toString(ch));
        }
    }

    private Token identifier() {
        while (isAlphaNumeric(peek())) advance();

        String id = new String(input, start, current-start, StandardCharsets.UTF_8)  ;
        TokenType type = keywords.get(id);
        if (type == null) type = IDENT;
        return new Token(type, id);
    }

    private Token number() {
        while (Character.isDigit(peek())) {
            advance();
        }
        
            String num = new String(input, start, current-start, StandardCharsets.UTF_8)  ;
            return new Token(NUMBER, num);
    }

    private Token string () {
        advance();
        start = current;
        while (peek() != '"' && peek() != 0) {
            advance();
        }
        String s = new String(input, start, current-start, StandardCharsets.UTF_8);
        Token token = new Token (TokenType.STRING,s);
        advance();
        return token;
    }

    private void advance()  {
        char ch = peek();
        if (ch != 0) {
            current++;
        }
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
                c == '_';
      }
    
      private boolean isAlphaNumeric(char c) {
        return isAlpha(c) || Character.isDigit((c));
      }
    

    private char peek () {
        if (current < input.length)
           return (char)input[current];
       return 0;
    }


    
}
