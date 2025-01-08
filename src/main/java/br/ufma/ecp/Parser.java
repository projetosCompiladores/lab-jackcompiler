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
import static br.ufma.ecp.token.TokenType.NOT;
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

import br.ufma.ecp.SymbolTable.Kind;
import br.ufma.ecp.SymbolTable.Symbol;
import br.ufma.ecp.VMWriter.Command;
import br.ufma.ecp.VMWriter.Segment;
import br.ufma.ecp.token.Token;
import br.ufma.ecp.token.TokenType;

public class Parser {

    private static class ParseError extends RuntimeException {
    }

    private Scanner scan;
    private Token currentToken;
    private Token peekToken;
    private StringBuilder xmlOutput = new StringBuilder();
    private VMWriter vmWriter = new VMWriter();
    private SymbolTable symTable = new SymbolTable();
    private int ifLabelNum = 0 ;
    private int whileLabelNum = 0;
    private String className = "";

    public Parser(byte[] input) {
        scan = new Scanner(input);
        vmWriter = new VMWriter();
        
        nextToken();
    }

    private void nextToken() {
        currentToken = peekToken;
        peekToken = scan.nextToken();
    }

    public void parse() {
        parseClass();
    }

    void parseClass() {
        printNonTerminal("class");
        expectPeek(CLASS);
        expectPeek(IDENT);
        className = currentToken.lexeme;
        expectPeek(LBRACE);

        while (peekTokenIs(STATIC) || peekTokenIs(FIELD)) {
            parseClassVarDec();
        }

        while (peekTokenIs(FUNCTION) || peekTokenIs(CONSTRUCTOR) || peekTokenIs(METHOD)) {
            parseSubroutineDec();
        }

        expectPeek(RBRACE);

        printNonTerminal("/class");
    }


    void parseClassVarDec() {
        printNonTerminal("classVarDec");
        expectPeek(FIELD, STATIC);

        SymbolTable.Kind kind = Kind.STATIC;
        if (currentTokenIs(FIELD))
            kind = Kind.FIELD;

        // 'int' | 'char' | 'boolean' | className
        expectPeek(INT, CHAR, BOOLEAN, IDENT);
        String type = currentToken.lexeme;

        expectPeek(IDENT);
        String name = currentToken.lexeme;

        symTable.define(name, type, kind);
        while (peekTokenIs(COMMA)) {
            expectPeek(COMMA);
            expectPeek(IDENT);

            name = currentToken.lexeme;
            symTable.define(name, type, kind);
        }

        expectPeek(SEMICOLON);
        printNonTerminal("/classVarDec");
    }

    void parseSubroutineDec() {
        printNonTerminal("subroutineDec");

        ifLabelNum = 0;
        whileLabelNum = 0;

        symTable.startSubroutine();

        expectPeek(CONSTRUCTOR, FUNCTION, METHOD);
        var subroutineType = currentToken.type;

        if (subroutineType == METHOD) {
            symTable.define("this", className, Kind.ARG);
        }

        // 'int' | 'char' | 'boolean' | className
        expectPeek(VOID, INT, CHAR, BOOLEAN, IDENT);
        expectPeek(IDENT);

        var functionName = className + "." + currentToken.lexeme;

        expectPeek(LPAREN);
        parseParameterList();
        expectPeek(RPAREN);
        parseSubroutineBody(functionName, subroutineType);

        printNonTerminal("/subroutineDec");
    }

    void parseParameterList() {
        printNonTerminal("parameterList");

        SymbolTable.Kind kind = Kind.ARG;

        if (!peekTokenIs(RPAREN)) // verifica se tem pelo menos uma expressao
        {
            expectPeek(INT, CHAR, BOOLEAN, IDENT);
            String type = currentToken.lexeme;

            expectPeek(IDENT);
            String name = currentToken.lexeme;
            symTable.define(name, type, kind);

            while (peekTokenIs(COMMA)) {
                expectPeek(COMMA);
                expectPeek(INT, CHAR, BOOLEAN, IDENT);
                type = currentToken.lexeme;

                expectPeek(IDENT);
                name = currentToken.lexeme;

                symTable.define(name, type, kind);
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

        SymbolTable.Kind kind = Kind.VAR;

        // 'int' | 'char' | 'boolean' | className
        expectPeek(INT, CHAR, BOOLEAN, IDENT);
        String type = currentToken.lexeme;

        expectPeek(IDENT);
        String name = currentToken.lexeme;
        symTable.define(name, type, kind);

        while (peekTokenIs(COMMA)) {
            expectPeek(COMMA);
            expectPeek(IDENT);

            name = currentToken.lexeme;
            symTable.define(name, type, kind);

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

        var isArray = false;

        printNonTerminal("letStatement");
        expectPeek(TokenType.LET);
        expectPeek(TokenType.IDENT);

        var symbol = symTable.resolve(currentToken.lexeme);

        if (peekTokenIs(TokenType.LBRACKET)) { // array
            expectPeek(LBRACKET);
            parseExpression();
            
            vmWriter.writePush(kind2Segment(symbol.kind()), symbol.index());
            vmWriter.writeArithmetic(Command.ADD);
    
            expectPeek(RBRACKET);



            isArray = true;
        }

        expectPeek(TokenType.EQ);
        parseExpression();

        if (isArray) {
            vmWriter.writePop(Segment.TEMP, 0);    // push result back onto stack
            vmWriter.writePop(Segment.POINTER, 1); // pop address pointer into pointer 1
            vmWriter.writePush(Segment.TEMP, 0);   // push result back onto stack
            vmWriter.writePop(Segment.THAT, 0);    // Store right hand side evaluation in THAT 0.
    

        } else {
            vmWriter.writePop(kind2Segment(symbol.kind()), symbol.index());
        }


        expectPeek(TokenType.SEMICOLON);
        printNonTerminal("/letStatement");
    }

    void parseIf() {
        printNonTerminal("ifStatement");

        var labelTrue = "IF_TRUE" + ifLabelNum;
        var labelFalse = "IF_FALSE" + ifLabelNum;
        var labelEnd = "IF_END" + ifLabelNum;

        ifLabelNum++;
    
        expectPeek(IF);
        expectPeek(LPAREN);
        parseExpression();
        expectPeek(RPAREN);

        vmWriter.writeIf(labelTrue);
        vmWriter.writeGoto(labelFalse);
        vmWriter.writeLabel(labelTrue);
    
        expectPeek(LBRACE);
        parseStatements();
        expectPeek(RBRACE);
        if (peekTokenIs(ELSE)){
            vmWriter.writeGoto(labelEnd);
        }

        vmWriter.writeLabel(labelFalse);

        if (peekTokenIs(ELSE))
        {
            expectPeek(ELSE);
            expectPeek(LBRACE);
            parseStatements();
            expectPeek(RBRACE);
            vmWriter.writeLabel(labelEnd);
        }

        printNonTerminal("/ifStatement");
    }

    void parseWhile() {
        printNonTerminal("whileStatement");

        var labelTrue = "WHILE_EXP" + whileLabelNum;
        var labelFalse = "WHILE_END" + whileLabelNum;
        whileLabelNum++;

        vmWriter.writeLabel(labelTrue);

        expectPeek(WHILE);
        expectPeek(LPAREN);
        parseExpression();

        vmWriter.writeArithmetic(Command.NOT);
        vmWriter.writeIf(labelFalse);

        expectPeek(RPAREN);
        expectPeek(LBRACE);
        parseStatements();

        vmWriter.writeGoto(labelTrue); // Go back to labelTrue and check condition
        vmWriter.writeLabel(labelFalse); // Breaks out of while loop because ~(condition) is true

        expectPeek(RBRACE);
        printNonTerminal("/whileStatement");
    }

    void parseDo() {
        printNonTerminal("doStatement");
        expectPeek(DO);
        expectPeek(IDENT);
        parseSubroutineCall();
        expectPeek(SEMICOLON);
        vmWriter.writePop(Segment.TEMP, 0);

        printNonTerminal("/doStatement");
    }

    void parseReturn() {
        printNonTerminal("returnStatement");
        expectPeek(RETURN);
        if (!peekTokenIs(SEMICOLON)) {
            parseExpression();
        } else {
            vmWriter.writePush(Segment.CONST, 0);
        }
        expectPeek(SEMICOLON);
        vmWriter.writeReturn();

        printNonTerminal("/returnStatement");
    }

    void parseExpression() {
        printNonTerminal("expression");
        parseTerm();
        while (peekTokenIs(PLUS, MINUS, ASTERISK, SLASH, LT, GT, EQ, AND, OR)) {
            var ope = peekToken.type;
            expectPeek(peekToken.type);
            parseTerm();
            compileOperators(ope);
        }
        printNonTerminal("/expression");
    }

    void parseTerm() {
        printNonTerminal("term");
        if (peekTokenIs(NUMBER)) {
            expectPeek(NUMBER);
            vmWriter.writePush(Segment.CONST, Integer.parseInt(currentToken.lexeme));
        } else if (peekTokenIs(STRING)) {
            expectPeek(STRING);
            var strValue = currentToken.lexeme;
            vmWriter.writePush(Segment.CONST, strValue.length());
            vmWriter.writeCall("String.new", 1);
            for (int i = 0; i < strValue.length(); i++) {
                vmWriter.writePush(Segment.CONST, strValue.charAt(i));
                vmWriter.writeCall("String.appendChar", 2);
            }        
        } else if (peekTokenIs(TRUE, FALSE, NULL)) {
            expectPeek(TRUE, FALSE, NULL);
            vmWriter.writePush(Segment.CONST, 0);
            if (currentToken.type == TRUE){
                vmWriter.writeArithmetic(Command.NOT);
            }
        } else if (peekTokenIs(THIS)) {
            expectPeek(THIS);
            vmWriter.writePush(Segment.POINTER, 0);

        } else if (peekTokenIs(MINUS, NOT)) {
            expectPeek(MINUS, NOT);
            var op = currentToken.type;
            parseTerm();
            if (op == MINUS)
                vmWriter.writeArithmetic(Command.NEG);
            else
                vmWriter.writeArithmetic(Command.NOT);
        } else if (peekTokenIs(IDENT)) {
            expectPeek(IDENT);
            Symbol sym = symTable.resolve(currentToken.lexeme);
            if (peekTokenIs(LPAREN, DOT)) {
                parseSubroutineCall();
            } else if (peekTokenIs(LBRACKET)) { // array
                expectPeek(LBRACKET);
                parseExpression();
                vmWriter.writePush(kind2Segment(sym.kind()), sym.index());
                vmWriter.writeArithmetic(Command.ADD);


                expectPeek(RBRACKET);
                vmWriter.writePop(Segment.POINTER, 1); // pop address pointer into pointer 1
                vmWriter.writePush(Segment.THAT, 0);   // push the value of the address pointer back onto stack

            } else {
                vmWriter.writePush(kind2Segment(sym.kind()), sym.index());
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
        

            var nArgs = 0;
    
            var ident = currentToken.lexeme;
            var symbol = symTable.resolve(ident); // classe ou objeto
            var functionName = ident + ".";
    
            if (peekTokenIs(LPAREN)) { // método da propria classe
                expectPeek(LPAREN);
                vmWriter.writePush(Segment.POINTER, 0);
                nArgs = parseExpressionList() + 1;
                expectPeek(RPAREN);
                functionName = className + "." + ident;
            } else {
                // pode ser um metodo de um outro objeto ou uma função
                expectPeek(DOT);
                expectPeek(IDENT); // nome da função
    
                if (symbol != null) { // é um metodo
                    functionName = symbol.type() + "." + currentToken.lexeme;
                    vmWriter.writePush(kind2Segment(symbol.kind()), symbol.index());
                    nArgs = 1; // do proprio objeto
                } else {
                    functionName += currentToken.lexeme; // é uma função
                }
    
                expectPeek(LPAREN);
                nArgs += parseExpressionList();
    
                expectPeek(RPAREN);
            }
    
            vmWriter.writeCall(functionName, nArgs);
      }

    int parseExpressionList() {
        printNonTerminal("expressionList");

        var nArgs = 0;

        if (!peekTokenIs(RPAREN)) // verifica se tem pelo menos uma expressao
        {
            parseExpression();
            nArgs = 1;
        }

        // procurando as demais
        while (peekTokenIs(COMMA)) {
            expectPeek(COMMA);
            parseExpression();
            nArgs++;
        }

        printNonTerminal("/expressionList");
        return nArgs;
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

    boolean currentTokenIs(TokenType type) {
        return currentToken.type == type;
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

    public void compileOperators(TokenType type) {

        if (type == ASTERISK) {
            vmWriter.writeCall("Math.multiply", 2);
        } else if (type == SLASH) {
            vmWriter.writeCall("Math.divide", 2);
        } else {
            vmWriter.writeArithmetic(typeOperator(type));
        }
    }

    private Command typeOperator(TokenType type) {
        if (type == PLUS)
            return Command.ADD;
        if (type == MINUS)
            return Command.SUB;
        if (type == LT)
            return Command.LT;
        if (type == GT)
            return Command.GT;
        if (type == EQ)
            return Command.EQ;
        if (type == AND)
            return Command.AND;
        if (type == OR)
            return Command.OR;
        return null;
    }

    public String VMOutput() {
        return vmWriter.vmOutput();
    }

    void parseSubroutineBody(String functionName, TokenType subroutineType) {

        printNonTerminal("subroutineBody");
        expectPeek(LBRACE);
        while (peekTokenIs(VAR)) {
            parseVarDec();
        }
				var nlocals = symTable.varCount(Kind.VAR);

        vmWriter.writeFunction(functionName, nlocals);

        parseStatements();
        expectPeek(RBRACE);
        printNonTerminal("/subroutineBody");
    }

    private Segment kind2Segment(Kind kind) {
        if (kind == Kind.STATIC)
            return Segment.STATIC;
        if (kind == Kind.FIELD)
            return Segment.THIS;
        if (kind == Kind.VAR)
            return Segment.LOCAL;
        if (kind == Kind.ARG)
            return Segment.ARG;
        return null;
    }
}
