/*
 * (C) Copyright 2013-2016 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.launcher.process;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SolarisProcessManager extends UnixProcessManager {

    protected static final String SOLARIS_11 = "5.11";

    protected static final String[] SOLARIS_11_PS = { "/usr/bin/ps", "auxww" };

    protected static final Pattern PS_OUTPUT_LINE = Pattern.compile("^" + "[^\\s]+\\s+" // USER
            + "([0-9]+)\\s+" // PID
            + "[0-9.\\s]+" // %CPU %MEM SZ RSS (may be collapsed)
            + "[^\\s]+\\s+" // TT (no starting digit)
            + "[^\\s]+\\s+" // S
            + "[^\\s]+\\s+" // START
            + "[^\\s]+\\s+" // TIME
            + "(.*)$" // COMMAND
    );

    protected String solarisVersion;

    protected SolarisProcessManager(Pattern processPattern, String solarisVersion) {
        super(processPattern);
        this.solarisVersion = solarisVersion;
    }

    @Override
    protected String[] psCommand() {
        if (SOLARIS_11.equals(solarisVersion)) {
            return SOLARIS_11_PS;
        }
        return null;
    }

    @Override
    protected Matcher lineMatcher(String line) {
        return PS_OUTPUT_LINE.matcher(line);
    }

    @Override
    public Optional<Long> findPid() throws IOException {
        if (SOLARIS_11.equals(solarisVersion)) {
            return super.findPid();
        } else {
            throw new RuntimeException("Unsupported Solaris version: " + solarisVersion);
        }
    }

    protected static String getSolarisVersion() {
        List<String> lines;
        try {
            lines = execute("/usr/bin/uname", "-r");
        } catch (IOException e) {
            lines = Collections.emptyList();
        }
        return lines.isEmpty() ? "?" : lines.get(0).trim();
    }

}
