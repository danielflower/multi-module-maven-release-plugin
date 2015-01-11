package scaffolding;

import org.apache.commons.exec.LogOutputStream;

import java.util.LinkedList;
import java.util.List;

public class CollectingLogOutputStream extends LogOutputStream {
    private final List<String> lines = new LinkedList<String>();

    @Override
    protected void processLine(String line, int level) {
        lines.add(line);
    }

    public List<String> getLines() {
        return lines;
    }
}
