package scaffolding;

import org.apache.commons.exec.LogOutputStream;

import java.util.LinkedList;
import java.util.List;

public class CollectingLogOutputStream extends LogOutputStream {
    private final List<String> lines = new LinkedList<String>();
    private final boolean logToStandardOut;

    public CollectingLogOutputStream(boolean logToStandardOut) {
        this.logToStandardOut = logToStandardOut;
    }

    @Override
    protected void processLine(String line, int level) {
        if (logToStandardOut) {
            System.out.println("        " + line);
        }
        lines.add(line);
    }

    public List<String> getLines() {
        return lines;
    }
}
