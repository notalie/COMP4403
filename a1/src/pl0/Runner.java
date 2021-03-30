package pl0;

import source.ErrorHandler;
import source.Errors;
import source.Source;
import tree.DeclNode;
import tree.StaticChecker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * class Runner holds run configuration used when running and compiling a PL0 program.
 */
public abstract class Runner {

    final Map<Character, Option> configurations = new LinkedHashMap<>();

    /**
     * Construct a Runner with default configuration.
     */
    public Runner() {
        configurations.put('d', new Option("turn debug messages on", false));
        configurations.put('s', new Option("turn off static checking", false));
        configurations.put('h', new Option("output this usage information", false));
    }

    /**
     * Set a configuration flag value. If the flag does not already exist, no
     * change will be made.
     *
     * @param flagCode Character representing the configuration.
     * @param value    New value for this configuration.
     */
    private void setFlag(Character flagCode, boolean value) {
        if (configurations.containsKey(flagCode)) {
            configurations.get(flagCode).set(value);
        }
    }

    /**
     * Query if a configuration flag is set.
     *
     * @param flagCode Character representing the configuration.
     * @return True iff the flag value is set.
     * False if flag is false or does not exist.
     */
    boolean isFlagSet(Character flagCode) {
        Option option = configurations.get(flagCode);
        if (option == null) {
            return false;
        }
        return option.isSet();
    }

    /**
     * Open and return a Source file.
     *
     * @param srcFile The file to load as a Source file.
     * @return The Source file.
     */
    private Source openSource(File srcFile) {
        try {
            return new Source(new FileInputStream(srcFile),
                    srcFile.getCanonicalPath());
        } catch (IOException io) {
            System.err.println("Unable to open source file " + srcFile);
            return null;
        }
    }

    /**
     * Parse a source file and generate an abstract syntax tree
     *
     * @param src The source file to parse
     * @return An abstract syntax tree
     */
    public abstract DeclNode.ProcedureNode parse(Source src);

    /**
     * Perform the static semantics analysis
     *
     * @param tree the abstract syntax tree to analyse
     * @return true iff the static check had no errors
     */
    private boolean staticCheck(DeclNode.ProcedureNode tree) {
        Errors errors = ErrorHandler.getErrorHandler();

        /* Perform static analysis on the abstract syntax tree */
        StaticChecker staticSemantics = new StaticChecker(errors);
        staticSemantics.visitProgramNode(tree);

        return !errors.hadErrors();
    }

    /**
     * Execute the abstract syntax tree.
     *
     * @param fileName The name of the PL0 program file.
     * @param tree   The abstract syntax tree to execute.
     * @param input  The input stream to the program.
     * @param output The output stream from the program.
     * @param errors Error handler for the program.
     * @return Whether the program terminated successfully.
     */
    public abstract boolean execute(String fileName,
                                    DeclNode.ProcedureNode tree,
                                    InputStream input, PrintStream output,
                                    Errors errors);

    /**
     * @return The usage instructions for the program
     */
    private String usage(String programName) {
        /* Convert flags with descriptions to a single string */
        StringBuilder flags = new StringBuilder();
        for (char character : configurations.keySet()) {
            flags.append(character);
        }

        StringBuilder builder = new StringBuilder();

        builder.append("PL0 Compiler").append(System.lineSeparator());

        builder.append("Usage: java ").append(programName)
                .append(" [-").append(flags).append("] <filename>")
                .append(System.lineSeparator());

        /* Provide a description for each of the flags */
        for (Map.Entry<Character, Option> flag : configurations.entrySet()) {
            builder.append("  -").append(flag.getKey()).append("  =  ")
                    .append(flag.getValue().getDescription())
                    .append(System.lineSeparator());
        }

        return builder.toString();
    }

    /**
     * Compile and run a source PL0 file
     *
     * @param fileName   the source PL0 file name
     * @param outStream stream to output the result of running the program to
     */
    public void run(String fileName, PrintStream outStream) {
        File srcFile = new File(fileName);
        Source source = openSource(srcFile);

        /* Failed to open source file, stop running */
        if (source == null) {
            return;
        }

        ErrorHandler errors = (ErrorHandler) ErrorHandler.getErrorHandler();
        errors.resetErrorHandler(outStream, source, isFlagSet('d'));

        outStream.println("Compiling " + new File(source.getFileName()).getName());

        /* Parse the source file to build a syntax tree */
        DeclNode.ProcedureNode tree = parse(source);

        errors.flush();
        outStream.println("Parsing complete");

        if (tree != null && !isFlagSet('s')) {
            /* if parsing was successful */
            /* Perform static semantic analysis on syntax tree */
            if (!staticCheck(tree)) { /* skip further steps if there were errors */
                tree = null;
            }
            errors.flush();
            outStream.println("Static semantic analysis complete");
        } else {
            tree = null;
        }

        if (tree != null) {
            /* Execute the abstract syntax tree */
            if (!execute(fileName, tree, System.in, outStream, errors)) {
                return;
            }
            outStream.println("\nTerminated");
        }
        errors.flush();
        errors.errorSummary();
    }

    /**
     * Parse arguments and set run configuration flags accordingly.
     *
     * @param args        list of arguments to a program
     * @param programName Name of the program - used for usage instructions
     * @param outStream   stream to output errors to
     * @return Name of the file passed as an argument
     */
    String parseArguments(String[] args, String programName,
                          PrintStream outStream) {
        /* Name of the input source program file. */
        String srcFile = null;

        /* Parse command line */
        for (String arg : args) {
            if (arg.charAt(0) == '-') { /* Option */
                char flag = arg.charAt(1);
                if (configurations.containsKey(flag)) {
                    /* Set the flag to the opposite of flag default */
                    setFlag(flag, true);
                } else {
                    /* Unknown flag given */
                    outStream.println("Unknown flag: " + flag);
                    setFlag('h', true);
                    break;
                }
            } else { /* (arg.charAt(0) != '-') Not Option */
                if (srcFile != null) {
                    /* Multiple source files */
                    outStream.println("Multiple source files specified.");
                    setFlag('h', true);
                    break;
                }
                srcFile = arg;
            }
        }

        if (isFlagSet('h')) {
            /* Output help message */
            outStream.println(usage(programName));
            System.exit(0);
        }

        if (srcFile == null) {
            outStream.println("No source file specified.");
            System.exit(1);
        }

        return srcFile;
    }
}


/**
 * Command line option for PL0 execution
 */
class Option {
    /**
     * Description of what effect the option has on the program
     */
    private final String description;
    /**
     * Whether or not the option has been set
     */
    private boolean set;

    /**
     * Construct a new option.
     *
     * @param description of what effect the option has on the program.
     * @param set         Whether or not the option has been set.
     */
    Option(String description, boolean set) {
        this.description = description;
        this.set = set;
    }

    /**
     * @return The description of what the option does
     */
    String getDescription() {
        return description;
    }

    /**
     * @return Whether the option is currently set
     */
    public boolean isSet() {
        return set;
    }

    /**
     * Set whether or not the option is set
     */
    public void set(boolean set) {
        this.set = set;
    }
}
