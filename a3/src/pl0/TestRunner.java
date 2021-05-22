package pl0;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Test that all programs in test-pgm produce the same output as in test-pgm/results.
 */
@RunWith(Parameterized.class)
public abstract class TestRunner {

    /**
     * Folder to search for test files
     **/
    private static final String SEARCH_FOLDER = "test-pgm";
    /**
     * Suffix of program files to look for
     **/
    private static final String PROGRAM_SUFFIX = ".pl0";
    /**
     * Prefix of the expected result file
     * File.separator used to specify that results should be in a results
     * directory in a platform independent way
     **/
    private static final String RESULTS_PREFIX = "results" + File.separator;
    /**
     * Suffix of the expected result file
     **/
    private static final String RESULTS_SUFFIX = ".txt";

    /**
     * PL0 source code currently being tested
     **/
    protected final File program;

    /**
     * Construct a new parameterized test instance
     *
     * @param program PL0 source code currently being tested
     */
    public TestRunner(File program) {
        this.program = program;
    }

    /**
     * Implemented by subclasses to run the program and print
     * output to outputStream
     */
    public abstract void run(PrintStream outputStream) throws IOException;

    /**
     * Compile and run the program and compare it with the expected results file
     */
    @Test
    public void test() throws Exception {
        /* Simulate an output stream to a byte array
         * Used to collect the output of a program as a string */
        ByteArrayOutputStream outputArray = new ByteArrayOutputStream();
        PrintStream outputStream = new PrintStream(outputArray, true);

        /* Compile and run an input file and output to the output stream */
        run(outputStream);

        /* Read all contents of the simulated output stream to a string */
        String output = new String(outputArray.toByteArray());
        /* Convert Windows line separator to Unix line separator */
        output = output.replaceAll("\\r\\n", "\n");
        /* Read all the contents of the result file to a string */
        String result = slurp(resultFile(program));
        result = result.replaceAll("\\r\\n", "\n");
        /* Compare the accumulated output strings */
        assertEquals("The outputs do not match", result, output);
    }

    /**
     * @return List all the of files in the search folder with the program suffix
     */
    @Parameterized.Parameters(name = "{0}")
    public static List<File> testPrograms() {
        /* Gather a list of all files in SEARCH_FOLDER with PROGRAM_SUFFIX suffix */
        File[] tests = new File(SEARCH_FOLDER)
                .listFiles(f -> f.isFile() && f.getName().endsWith(PROGRAM_SUFFIX));

        if (tests == null) {
            tests = new File[]{};
        }
        return Arrays.asList(tests);
    }

    /**
     * @return The expected result file for a given test program
     */
    private static File resultFile(File src) {
        File parent = src.getParentFile();
        /* Get the path to the folder one level above the test file
         * If there is no parent folder, leave empty */
        String parentPath = parent == null ? "" : parent.getAbsolutePath() + File.separator;

        /* Get the name of the file with the file extension removed */
        String filename = src.getName();
        if (filename.contains(".")) {
            /* Remove the extension */
            filename = filename.substring(0, filename.lastIndexOf('.'));
        }

        return new File(parentPath + RESULTS_PREFIX + filename + RESULTS_SUFFIX);
    }

    /**
     * @return Read and return an entire file as a string
     */
    private String slurp(File src) throws IOException {
        return new String(Files.readAllBytes(src.toPath()));
    }
}
