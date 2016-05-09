package scaffolding;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;

import java.util.List;

public class MavenExecutionException extends RuntimeException {
    public final int exitCode;
    public final List<String> output;

    public MavenExecutionException(int exitCode, List<String> output) {
        super("Error from mvn: " + output);
        this.exitCode = exitCode;
        this.output = output;
    }

    @Override
    public String toString() {
        return "MavenExecutionException{" +
            "exitCode=" + exitCode +
            ", output=" + StringUtils.join(output.toArray(), SystemUtils.LINE_SEPARATOR);
    }
}
