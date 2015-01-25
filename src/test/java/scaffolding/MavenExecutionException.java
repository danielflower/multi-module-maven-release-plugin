package scaffolding;

import java.util.List;

public class MavenExecutionException extends RuntimeException {
    public final int exitCode;
    public final List<String> output;

    public MavenExecutionException(int exitCode, List<String> output) {
        super("Error from mvn: " + output);
        this.exitCode = exitCode;
        this.output = output;
    }
}
