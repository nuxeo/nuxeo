//
// JODConverter - Java OpenDocument Converter
// Copyright 2009 Art of Solving Ltd
// Copyright 2004-2009 Mirko Nasato
//
// JODConverter is free software: you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public License
// as published by the Free Software Foundation, either version 3 of
// the License, or (at your option) any later version.
//
// JODConverter is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General
// Public License along with JODConverter.  If not, see
// <http://www.gnu.org/licenses/>.
//
package org.nuxeo.launcher.process;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

/**
 * {@link ProcessManager} implementation for *nix systems. Uses the <tt>ps</tt> and <tt>kill</tt> commands.
 * <p>
 * Works for Linux. Works for Solaris too, except that the command line string returned by <tt>ps</tt> there is limited
 * to 80 characters and this affects {@link #findPid()}.
 */
public class UnixProcessManager extends ProcessManager {

    private static final Pattern PS_OUTPUT_LINE = Pattern.compile("^\\s*(\\d+)\\s+(.*)$");

    protected UnixProcessManager(Pattern processPattern) {
        super(processPattern);
    }

    protected String[] psCommand() {
        return new String[] { "/bin/ps", "-e", "-o", "pid,args" };
    }

    @Override
    public Optional<Long> findPid() throws IOException {
        for (String line : execute0(psCommand())) {
            Matcher lineMatcher = lineMatcher(line);
            if (lineMatcher.matches()) {
                String command = lineMatcher.group(2);
                Matcher commandMatcher = processPattern.matcher(command);
                if (commandMatcher.find()) {
                    return Optional.ofNullable(lineMatcher.group(1)).map(Long::valueOf);
                }
            }
        }
        return super.findPid();
    }

    protected Matcher lineMatcher(String line) {
        return PS_OUTPUT_LINE.matcher(line);
    }

    @Override
    public void kill(ProcessHandle processHandle) throws IOException {
        execute0("/bin/kill", "-KILL", String.valueOf(processHandle.pid()));
    }

    // non-static method to allow tests to mock it
    protected List<String> execute0(String... command) throws IOException {
        return execute(command);
    }

    protected static List<String> execute(String... command) throws IOException {
        Process process = new ProcessBuilder(command).start();
        return IOUtils.readLines(process.getInputStream(), UTF_8);
    }

}
