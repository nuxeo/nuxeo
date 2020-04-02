package org.nuxeo.launcher.process;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

/**
 * {@link ProcessManager} implementation for Windows.
 * <p>
 * Requires wmic.exe and taskkill.exe, that should be available at least on Windows XP, Windows Vista, and Windows 7
 * (except Home versions).
 */
public class WindowsProcessManager extends ProcessManager {

    private static final Pattern PROCESS_GET_LINE = Pattern.compile("^(.*?)\\s+(\\d+)\\s*$");

    protected WindowsProcessManager(Pattern processPattern) {
        super(processPattern);
    }

    @Override
    public Optional<Long> findPid() throws IOException {
        for (String line : execute("wmic", "process", "get", "CommandLine,ProcessId")) {
            Matcher lineMatcher = PROCESS_GET_LINE.matcher(line);
            if (lineMatcher.matches()) {
                String commandLine = lineMatcher.group(1);
                String pid = lineMatcher.group(2);
                Matcher commandMatcher = processPattern.matcher(commandLine);
                if (commandMatcher.find()) {
                    return Optional.ofNullable(pid).map(Long::valueOf);
                }
            }
        }
        return super.findPid();
    }

    @Override
    public void kill(ProcessHandle processHandle) throws IOException {
        execute("taskkill", "/t", "/f", "/pid", String.valueOf(processHandle.pid()));
    }

    private static List<String> execute(String... command) throws IOException {
        Process process = new ProcessBuilder(command).start();
        process.getOutputStream().close(); // don't wait for stdin
        List<String> lines = IOUtils.readLines(process.getInputStream(), UTF_8);
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        return lines;
    }

    protected static boolean isUsable() {
        try {
            execute("wmic", "quit");
            execute("taskkill", "/?");
            return true;
        } catch (IOException ioException) {
            return false;
        }
    }

}
