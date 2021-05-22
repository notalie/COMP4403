package pl0;

import java_cup.runtime.ComplexSymbolFactory;
import machine.StackMachine;
import parse.CUPParser;
import source.Errors;
import source.Source;
import tree.CodeGenerator;
import tree.DeclNode;
import tree.Procedures;

import java.io.File;
import java.io.InputStream;
import java.io.PrintStream;

/**
 * class PL0_LALR - PL0 Compiler with JavaCUP generated parser.
 * Parses the command line arguments, and then compiles and/or executes the
 * code.
 */
public class PL0_LALR extends Runner {

    /**
     * Name of the program being executed - used for usage
     **/
    private static final String PROGRAM_NAME = "pl0.PL0_LALR";

    public PL0_LALR() {
        /* Setup the command line configuration flags */
        configurations.put('t', new Option("trace execution of resulting code", false));
        configurations.put('v', new Option("output of generated code", false));
        configurations.put('g', new Option("turn code generation off", false));
        configurations.put('e', new Option("turn stack machine execution off", false));
    }

    @Override
    public DeclNode.ProcedureNode parse(Source src) {
        /* Abstract syntax tree returned by parser. Really of type
         * StatementNode.ProcedureNode but the parser generator doesn't know that. */
        Object parseResult;

        ComplexSymbolFactory csf = new ComplexSymbolFactory();
        /* Set up the lexical analyzer using the source program stream */
        parse.Lexer lex = new parse.Lexer(src, csf);
        /* Generated parser.
         * Set up the parser with the lexical analyzer. */
        CUPParser parser = new CUPParser(lex, csf);
        try {
            if (isFlagSet('d')) {
                /* Parse the source with debugging on */
                parseResult = parser.debug_parse().value;
            } else {
                /* Parse the source with no debugging */
                parseResult = parser.parse().value;
            }
        } catch (Exception e) {
            System.out.println("Exception: " + e + "... Aborting");
            System.exit(1);
            return null;
        }
        if (parseResult instanceof DeclNode.ProcedureNode) {
            return (DeclNode.ProcedureNode) parseResult;
        } else {
            /* unrecoverable syntax error may return garbage object */
            return null;
        }
    }

    /**
     * Compile the abstract syntax tree into procedures to execute
     * in the stack machine
     */
    private Procedures compile(DeclNode.ProcedureNode tree, Errors errors) {
        CodeGenerator generator = new CodeGenerator(errors);
        return generator.generateCode(tree);
    }

    @Override
    public boolean execute(DeclNode.ProcedureNode tree, InputStream input,
                           PrintStream output, Errors errors) {
        /* Prevent compiling and executing if code generation flag is off */
        if (isFlagSet('g')) {
            return false;
        }

        /* Compile syntax tree to code */
        Procedures code = compile(tree, errors);
        output.println("Code generation complete");

        if (code != null) { /* run it if possible */
            /* Prevent executing if execute flag is off */
            if (isFlagSet('e')) {
                return true;
            }

            /* Run compiled code on stack machine */
            StackMachine machine = new StackMachine(errors, output,
                    isFlagSet('v'), code);
            output.println("Running ...");
            machine.setTracing(isFlagSet('t') ? StackMachine.TRACE_ALL
                    : StackMachine.TRACE_NONE);
            machine.run();

            return true;
        }

        /* Return false if it failed to compile */
        return false;
    }

    /**
     * PL0 LALR main procedure
     */
    public static void main(String[] args) {
        Runner runner = new PL0_LALR();

        /* Parse the command line arguments to set flags and find the source file parameter */
        String srcFile = runner.parseArguments(args, PROGRAM_NAME, System.out);

        /* Run an input file and output to the output stream */
        runner.run(new File(srcFile), System.out);
    }
}
