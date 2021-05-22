package source;

import java_cup.runtime.ComplexSymbolFactory.Location;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * class Source - Handles the input character-by-character.
 * To interface with JFlex this class has to extend java.io.Reader.
 */
public class Source extends java.io.Reader {

    /**
     * Name of the input source file.
     */
    private final String fileName;
    /**
     * Buffered reader for input source file
     */
    private final BufferedReader input;
    /**
     * Provides the locations of the end of every line.
     */
    private final LineLocations lineLocations;
    /**
     * Current location in the input source file.
     */
    private int currentLoc;

    public Source(String filename)
            throws java.io.IOException {
        this(new FileInputStream(filename), filename);
    }

    public Source(InputStream in, String inFile) {
        input = new BufferedReader(new InputStreamReader(in));
        fileName = inFile;
        currentLoc = 0;
        lineLocations = new LineLocations();
    }

    public String getFileName() {
        return fileName;
    }

    /* Close input stream any flush out any error messages */
    public void close() throws IOException {
        input.close();
    }

    /**
     * Get the location of the start of the line containing loc.
     */
    Integer getLineStart(Location loc) {
        return lineLocations.getLineStart(loc);
    }

    /**
     * Provides buffered read to JFlex.
     * getNextChar should be enough, but this is the interface JFlex wants.
     */
    public int read(char[] buffer, int off, int len) throws IOException {
        int chars_read = input.read(buffer, off, len);
        if (chars_read < 0) {
            lineLocations.add(currentLoc);
        } else {
            for (int i = 0; i < chars_read; i++) {
                if (buffer[off + i] == '\n') {
                    lineLocations.add(currentLoc);
                }
                currentLoc++;
            }
        }
        return chars_read;
    }
}
