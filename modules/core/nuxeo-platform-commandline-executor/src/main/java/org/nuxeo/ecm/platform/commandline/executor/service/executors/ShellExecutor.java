/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 *     Julien Carsique
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.commandline.executor.service.executors;

import static org.apache.commons.io.IOUtils.buffer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters.ParameterValue;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.commandline.executor.service.CommandLineDescriptor;
import org.nuxeo.ecm.platform.commandline.executor.service.EnvironmentDescriptor;
import org.nuxeo.runtime.RuntimeServiceException;

/**
 * Default implementation of the {@link Executor} interface. Use simple shell exec.
 */
public class ShellExecutor implements Executor {

    private static final Logger log = LogManager.getLogger(ShellExecutor.class);

    protected static final AtomicInteger PIPE_COUNT = new AtomicInteger();

    /** Used to split the contributed command, NOT the passed parameter values. */
    protected static final Pattern COMMAND_SPLIT = Pattern.compile("\"([^\"]*)\"|'([^']*)'|[^\\s]+");

    @Override
    public ExecResult exec(CommandLineDescriptor cmdDesc, CmdParameters params, EnvironmentDescriptor env) {
        String commandLine = cmdDesc.getCommand() + " " + String.join(" ", cmdDesc.getParametersString());
        try {
            log.debug("Running system command: {} with parameters: {}", () -> commandLine,
                    () -> params.getParameters()
                                .entrySet()
                                .stream()
                                .map(e -> String.format("%s=%s", e.getKey(), e.getValue().getValue()))
                                .collect(Collectors.joining(", ")));
            long t0 = System.currentTimeMillis();
            ExecResult res = exec1(cmdDesc, params, env);
            long t1 = System.currentTimeMillis();
            return new ExecResult(commandLine, res.getOutput(), t1 - t0, res.getReturnCode());
        } catch (IOException e) {
            return new ExecResult(commandLine, e);
        }
    }

    protected ExecResult exec1(CommandLineDescriptor cmdDesc, CmdParameters params, EnvironmentDescriptor env)
            throws IOException {
        // split the configured parameters while keeping quoted parts intact
        List<String> list = new ArrayList<>();
        list.add(cmdDesc.getCommand());
        Matcher m = COMMAND_SPLIT.matcher(cmdDesc.getParametersString());
        while (m.find()) {
            String word;
            if (m.group(1) != null) {
                word = m.group(1); // double-quoted
            } else if (m.group(2) != null) {
                word = m.group(2); // single-quoted
            } else {
                word = m.group(); // word
            }
            List<String> words = replaceParams(word, params);
            list.addAll(words);
        }

        List<ProcessBuilder> builders = new LinkedList<>();
        List<String> command = new LinkedList<>();
        for (Iterator<String> it = list.iterator(); it.hasNext();) {
            String word = it.next();
            boolean build;
            if (word.equals("|")) {
                build = true;
            } else {
                // on Windows, look up the command in the PATH first
                if (command.isEmpty() && SystemUtils.IS_OS_WINDOWS) {
                    command.add(getCommandAbsolutePath(word));
                } else {
                    command.add(word);
                }
                build = !it.hasNext();
            }
            if (!build) {
                continue;
            }
            var processBuilder = createProcessBuilder(command, env);
            builders.add(processBuilder);

            command = new LinkedList<>(); // reset for next loop
        }
        // now start all process
        List<Process> processes = ProcessBuilder.startPipeline(builders);

        // get result from last process
        List<String> output;
        Process last = processes.get(processes.size() - 1);
        try (var stream = buffer(last.getInputStream())) {
            output = IOUtils.readLines(stream, Charset.defaultCharset()); // use the host charset
        }

        // get return code from processes
        int returnCode = getReturnCode(processes);

        return new ExecResult(null, output, 0, returnCode);
    }

    protected ProcessBuilder createProcessBuilder(List<String> command, EnvironmentDescriptor env) {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        log.debug("Building Process for command: {}", () -> String.join(" ", processBuilder.command()));
        processBuilder.directory(new File(env.getWorkingDirectory()));
        processBuilder.environment().putAll(env.getParameters());
        processBuilder.redirectErrorStream(true);
        return processBuilder;
    }

    protected int getReturnCode(List<Process> processes) {
        // wait for all processes, get first non-0 exit status
        int returnCode = 0;
        for (Process p : processes) {
            try {
                int exitCode = p.waitFor();
                if (returnCode == 0) {
                    returnCode = exitCode;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeServiceException(e);
            }
        }
        return returnCode;
    }

    /**
     * Returns a started daemon thread piping bytes from the InputStream to the OutputStream.
     * <p>
     * The streams are both closed when the copy is finished.
     *
     * @since 7.10
     * @deprecated since 11.1, seems unused
     */
    @Deprecated(since = "11.1")
    public static Thread pipe(InputStream in, OutputStream out) {
        Runnable run = () -> {
            try (in; out) {
                IOUtils.copy(in, out);
                out.flush();
            } catch (IOException e) {
                throw new RuntimeServiceException(e);
            }
        };
        Thread thread = new Thread(run, "Nuxeo-pipe-" + PIPE_COUNT.incrementAndGet());
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /**
     * Expands parameter strings in a parameter word.
     * <p>
     * This may return several words if the parameter value is marked as a list.
     *
     * @since 7.10
     */
    public static List<String> replaceParams(String word, CmdParameters params) {
        for (Entry<String, ParameterValue> es : params.getParameters().entrySet()) {
            String name = es.getKey();
            ParameterValue paramVal = es.getValue();
            String param = "#{" + name + "}";
            if (paramVal.isMulti()) {
                if (word.equals(param)) {
                    return paramVal.getValues();
                }
            } else if (word.contains(param)) {
                word = word.replace(param, paramVal.getValue());
            }

        }
        return Collections.singletonList(word);
    }

    /**
     * Returns the absolute path of a command looked up on the PATH or the initial string if not found.
     *
     * @since 7.10
     */
    public static String getCommandAbsolutePath(String command) {
        // no lookup if the command is already an absolute path
        if (Paths.get(command).isAbsolute()) {
            return command;
        }
        List<String> extensions = Arrays.asList("", ".exe");
        // lookup for "command" or "command.exe" in the PATH
        String[] systemPaths = System.getenv("PATH").split(File.pathSeparator);
        for (String ext : extensions) {
            for (String sp : systemPaths) {
                String fullCommand = command + ext;
                try {
                    Path path = Paths.get(sp.trim());
                    if (Files.exists(path.resolve(fullCommand))) {
                        return path.resolve(fullCommand).toString();
                    }
                } catch (InvalidPathException e) {
                    log.warn("PATH environment variable contains an invalid path: {}", fullCommand, e);
                }
            }
        }
        // not found : return the initial string
        return command;
    }

}
