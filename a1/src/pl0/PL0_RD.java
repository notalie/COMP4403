package pl0;

import interpreter.Interpreter;
import parse.Parser;
import parse.Scanner;
import parse.TokenStream;
import source.Errors;
import source.Source;
import tree.DeclNode;

import java.io.InputStream;
import java.io.PrintStream;

/**
 * class PL0_RD - PL0 Compiler with recursive descent parser.
 * Parses the command line arguments, and then compiles and/or executes the
 * code.
 */
public class PL0_RD extends Runner {

    /**
     * Name of the program being executed - used for usage
     **/
    private static final String PROGRAM_NAME = "pl0.PL0_RD";

    public PL0_RD() {
        configurations.put('i', new Option("turn off interpreting", false));
    }

    @Override
    public DeclNode.ProcedureNode parse(Source src) {
        DeclNode.ProcedureNode result;
        /* Set up the lexical analyzer using the source program stream */
        Scanner lex = new Scanner(src);
        /* Recursive descent parser.
         * Set up the parser with the lexical analyzer. */
        TokenStream tokens = new TokenStream(lex);
        Parser parser = new Parser(tokens);
        result = parser.parseMain();
        return result;
    }

    @Override
    public boolean execute(String fileName, DeclNode.ProcedureNode tree, InputStream input,
                           PrintStream output, Errors errors) {
        if (isFlagSet('i')) {
            return false;
        }

        //output.println("Running " + fileName); // Larissa changed so that tests work
        output.println("Running ...");
        Interpreter interpreter = new Interpreter(errors, input, output);
        try {
            interpreter.executeCode(tree);
        } catch (Error error) {
            return false;
        }
        return true;
    }

    /**
     * PL0 Recursive Decent main procedure
     */
    public static void main(String[] args) {
        Runner runner = new PL0_RD();

        /* Parse the command line arguments to set flags and find the source file parameter */
        String fileName = runner.parseArguments(args, PROGRAM_NAME, System.out);

        /* Run an input file and output to the output stream */
        runner.run(fileName, System.out);
    }
}
