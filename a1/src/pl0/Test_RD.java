package pl0;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

public class Test_RD extends TestRunner {

    /**
     * Construct a new parameterized test instance
     *
     * @param program PL0 source code currently being tested
     */
    public Test_RD(File program) {
        super(program);
    }

    @Override
    public void run(PrintStream outputStream) throws IOException {
        Runner runner = new PL0_RD();
        runner.run(program.getCanonicalPath(), outputStream);
    }
}
