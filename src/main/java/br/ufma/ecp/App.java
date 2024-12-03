package br.ufma.ecp;

import static br.ufma.ecp.token.TokenType.EOF;

import br.ufma.ecp.token.Token;

public class App {

    public static void main(String[] args) {

        String input = """
            let a = 10;
            do print(a);
            """;

        Parser parser = new Parser(input.getBytes());
        parser.parseStatement();

        Scanner scan = new Scanner(input.getBytes());
        for (Token tk = scan.nextToken(); tk.type != EOF; tk = scan.nextToken()) {
            System.out.println(tk);
        }
    }
}
