/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.launcher.process;

import java.io.IOException;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.lang3.SystemUtils;

/**
 * @since 11.1
 */
public class ProcessManager {

    protected final Pattern processPattern;

    protected ProcessManager(Pattern processPattern) {
        this.processPattern = processPattern;
    }

    /**
     * Finds the pid associated to the regex used to build this manager.
     */
    public Optional<Long> findPid() throws IOException {
        try {
            return ProcessHandle.allProcesses()
                                .filter(ph -> processPattern.matcher(ph.info().commandLine().orElse("")).matches())
                                .map(ProcessHandle::pid)
                                .findFirst();
        } catch (UnsupportedOperationException e) {
            throw new IOException("Your system doesn't support looking up of process", e);
        }
    }

    /**
     * Kills the given process.
     */
    public void kill(ProcessHandle processHandle) throws IOException {
        processHandle.destroy();
    }

    /**
     * @since 11.1
     */
    public static ProcessManager of(String commandRegex) {
        var commandPattern = Pattern.compile(commandRegex);
        ProcessManager manager;
        if (SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_AIX) {
            manager = new UnixProcessManager(commandPattern);
        } else if (SystemUtils.IS_OS_MAC) {
            manager = new MacProcessManager(commandPattern);
        } else if (SystemUtils.IS_OS_SUN_OS) {
            var solarisVersion = SolarisProcessManager.getSolarisVersion();
            manager = new SolarisProcessManager(commandPattern, solarisVersion);
        } else if (SystemUtils.IS_OS_WINDOWS) {
            manager = WindowsProcessManager.isUsable() ? new WindowsProcessManager(commandPattern)
                    : new ProcessManager(commandPattern);
        } else {
            manager = new ProcessManager(commandPattern);
        }
        return manager;
    }

}
