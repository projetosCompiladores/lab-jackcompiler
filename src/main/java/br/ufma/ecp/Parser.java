package br.ufma.ecp;

import static br.ufma.ecp.token.TokenType.AND;
import static br.ufma.ecp.token.TokenType.ASTERISK;
import static br.ufma.ecp.token.TokenType.BOOLEAN;
import static br.ufma.ecp.token.TokenType.CHAR;
import static br.ufma.ecp.token.TokenType.CLASS;
import static br.ufma.ecp.token.TokenType.COMMA;
import static br.ufma.ecp.token.TokenType.CONSTRUCTOR;
import static br.ufma.ecp.token.TokenType.DO;
import static br.ufma.ecp.token.TokenType.DOT;
import static br.ufma.ecp.token.TokenType.ELSE;
import static br.ufma.ecp.token.TokenType.EQ;
import static br.ufma.ecp.token.TokenType.FALSE;
import static br.ufma.ecp.token.TokenType.FIELD;
import static br.ufma.ecp.token.TokenType.FUNCTION;
import static br.ufma.ecp.token.TokenType.GT;
import static br.ufma.ecp.token.TokenType.IDENT;
import static br.ufma.ecp.token.TokenType.IF;
import static br.ufma.ecp.token.TokenType.INT;
import static br.ufma.ecp.token.TokenType.LBRACE;
import static br.ufma.ecp.token.TokenType.LBRACKET;
import static br.ufma.ecp.token.TokenType.LET;
import static br.ufma.ecp.token.TokenType.LPAREN;
import static br.ufma.ecp.token.TokenType.LT;
import static br.ufma.ecp.token.TokenType.METHOD;
import static br.ufma.ecp.token.TokenType.MINUS;
import static br.ufma.ecp.token.TokenType.NULL;
import static br.ufma.ecp.token.TokenType.NUMBER;
import static br.ufma.ecp.token.TokenType.OR;
import static br.ufma.ecp.token.TokenType.PLUS;
import static br.ufma.ecp.token.TokenType.RBRACE;
import static br.ufma.ecp.token.TokenType.RBRACKET;
import static br.ufma.ecp.token.TokenType.RETURN;
import static br.ufma.ecp.token.TokenType.RPAREN;
import static br.ufma.ecp.token.TokenType.SEMICOLON;
import static br.ufma.ecp.token.TokenType.SLASH;
import static br.ufma.ecp.token.TokenType.STATIC;
import static br.ufma.ecp.token.TokenType.STRING;
import static br.ufma.ecp.token.TokenType.THIS;
import static br.ufma.ecp.token.TokenType.TRUE;
import static br.ufma.ecp.token.TokenType.VAR;
import static br.ufma.ecp.token.TokenType.VOID;
import static br.ufma.ecp.token.TokenType.WHILE;

import br.ufma.ecp.token.Token;
import br.ufma.ecp.token.TokenType;

public class Parser {

    private static class ParseError extends RuntimeException {
    }

    private Scanner scan;
    private Token currentToken;
    private Token peekToken;
    private StringBuilder xmlOutput = new StringBuilder();

    public Parser(byte[] input) {
        scan = new Scanner(input);
        nextToken();
    }

    private void nextToken() {
        currentToken = peekToken;
        peekToken = scan.nextToken();
    }

    public void parse() {
        printNonTerminal("class");
        expectPeek(CLASS);
        expectPeek(IDENT);
        expectPeek(LBRACE);

        while (peekTokenIs(STATIC, FIELD)) {
            parseClassVarDec();
        }
        while (peekTokenIs(CONSTRUCTOR, FUNCTION, METHOD)) {
            parseSubroutineDec();
        }
        expectPeek(RBRACE);
        printNonTerminal("/class");
    }

    public void parseClassVarDec() {
        printNonTerminal("classVarDec");
        expectPeek(STATIC, FIELD);
        expectPeek(INT, CHAR, BOOLEAN, IDENT);
        expectPeek(IDENT);
        while (peekTokenIs(COMMA)) {
            expectPeek(COMMA);
            expectPeek(IDENT);
        }
        expectPeek(SEMICOLON);
        printNonTerminal("/classVarDec");
    }

    public void parseSubroutineDec() {
        printNonTerminal("subroutineDec");
        expectPeek(CONSTRUCTOR, FUNCTION, METHOD);
        expectPeek(VOID, INT, CHAR, BOOLEAN, IDENT);
        expectPeek(IDENT);
        expectPeek(LPAREN);
        parseParameterList();
        expectPeek(RPAREN);
        parseSubroutineBody();
        printNonTerminal("/subroutineDec");
    }

    void parseParameterList() {
        printNonTerminal("parameterList");
        if (peekTokenIs(INT, CHAR, BOOLEAN, IDENT)) {
            expectPeek(INT, CHAR, BOOLEAN, IDENT);
            expectPeek(IDENT);
            while (peekTokenIs(COMMA)) {
                expectPeek(COMMA);
                expectPeek(INT, CHAR, BOOLEAN, IDENT);
                expectPeek(IDENT);
            }
        }
        printNonTerminal("/parameterList");
    }

    void parseSubroutineBody() {
        printNonTerminal("subroutineBody");
        expectPeek(LBRACE);
        while (peekTokenIs(VAR)) {
            parseVarDec();
        }
        parseStatements();
        expectPeek(RBRACE);
        printNonTerminal("/subroutineBody");
    }

    void parseVarDec() {
        printNonTerminal("varDec");
        expectPeek(VAR);
        expectPeek(INT, CHAR, BOOLEAN, IDENT);
        expectPeek(IDENT);
        while (peekTokenIs(COMMA)) {
            expectPeek(COMMA);
            expectPeek(IDENT);
        }
        expectPeek(SEMICOLON);
        printNonTerminal("/varDec");
    }

    public void parseStatements() {
        printNonTerminal("statements");
        while (peekTokenIs(LET, IF, WHILE, DO, RETURN)) {
            parseStatement();
        }
        printNonTerminal("/statements");
    }

    public void parseStatement() {
        if (peekTokenIs(LET)) {
            parseLet();
        } else if (peekTokenIs(IF)) {
            parseIf();
        } else if (peekTokenIs(WHILE)) {
            parseWhile();
        } else if (peekTokenIs(DO)) {
            parseDo();
        } else if (peekTokenIs(RETURN)) {
            parseReturn();
        } else {
            throw error(peekToken, "Expected a statement");
        }
    }

    void parseLet() {
        printNonTerminal("letStatement");
        expectPeek(LET);
        expectPeek(IDENT);
        if (peekTokenIs(LBRACKET)) {
            expectPeek(LBRACKET);
            parseExpression();
            expectPeek(RBRACKET);
        }
        expectPeek(EQ);
        parseExpression();
        expectPeek(SEMICOLON);
        printNonTerminal("/letStatement");
    }

    void parseIf() {
        printNonTerminal("ifStatement");
        expectPeek(IF);
        expectPeek(LPAREN);
        parseExpression();
        expectPeek(RPAREN);
        expectPeek(LBRACE);
        parseStatements();
        expectPeek(RBRACE);
        if (peekTokenIs(ELSE)) {
            expectPeek(ELSE);
            expectPeek(LBRACE);
            parseStatements();
            expectPeek(RBRACE);
        }
        printNonTerminal("/ifStatement");
    }

    void parseWhile() {
        printNonTerminal("whileStatement");
        expectPeek(WHILE);
        expectPeek(LPAREN);
        parseExpression();
        expectPeek(RPAREN);
        expectPeek(LBRACE);
        parseStatements();
        expectPeek(RBRACE);
        printNonTerminal("/whileStatement");
    }

    void parseDo() {
        printNonTerminal("doStatement");
        expectPeek(DO);
        parseSubroutineCall();
        expectPeek(SEMICOLON);
        printNonTerminal("/doStatement");
    }

    void parseReturn() {
        printNonTerminal("returnStatement");
        expectPeek(RETURN);
        if (!peekTokenIs(SEMICOLON)) {
            parseExpression();
        }
        expectPeek(SEMICOLON);
        printNonTerminal("/returnStatement");
    }

    void parseExpression() {
        printNonTerminal("expression");
        parseTerm();
        while (peekTokenIs(PLUS, MINUS, ASTERISK, SLASH, LT, GT, EQ, AND, OR)) {
            expectPeek(peekToken.type);
            parseTerm();
        }
        printNonTerminal("/expression");
    }

    void parseTerm() {
        printNonTerminal("term");
        if (peekTokenIs(NUMBER)) {
            expectPeek(NUMBER);
        } else if (peekTokenIs(STRING)) {
            expectPeek(STRING);
        } else if (peekTokenIs(TRUE, FALSE, NULL, THIS)) {
            expectPeek(TRUE, FALSE, NULL, THIS);
        } else if (peekTokenIs(IDENT)) {
            expectPeek(IDENT);
            if (peekTokenIs(LPAREN, DOT)) {
                parseSubroutineCall();
            } else if (peekTokenIs(LBRACKET)) {
                expectPeek(LBRACKET);
                parseExpression();
                expectPeek(RBRACKET);
            }
        } else if (peekTokenIs(LPAREN)) {
            expectPeek(LPAREN);
            parseExpression();
            expectPeek(RPAREN);
        } else {
            throw error(peekToken, "term expected");
        }
        printNonTerminal("/term");
    }

    void parseSubroutineCall() {
        expectPeek(IDENT);
        if (peekTokenIs(DOT)) {
            expectPeek(DOT);
            expectPeek(IDENT);
        }
        expectPeek(LPAREN);
        parseExpressionList();
        expectPeek(RPAREN);
    }

    void parseExpressionList() {
        printNonTerminal("expressionList");
        if (!peekTokenIs(RPAREN)) {
            parseExpression();
            while (peekTokenIs(COMMA)) {
                expectPeek(COMMA);
                parseExpression();
            }
        }
        printNonTerminal("/expressionList");
    }

    public String XMLOutput() {
        return xmlOutput.toString();
    }

    private void printNonTerminal(String nterminal) {
        xmlOutput.append(String.format("<%s>\r\n", nterminal));
    }

    boolean peekTokenIs(TokenType... types) {
        for (TokenType type : types) {
            if (peekToken.type == type) {
                return true;
            }
        }
        return false;
    }

    private void expectPeek(TokenType... types) {
        for (TokenType type : types) {
            if (peekToken.type == type) {
                nextToken();
                xmlOutput.append(String.format("%s\r\n", currentToken.toString()));
                return;
            }
        }
        throw error(peekToken, "Expected a statement");
    }

    private ParseError error(Token token, String message) {
        throw new ParseError();
    }
}
