/*
 * (C) Copyright 2006-2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thierry Delprat
 *     Julien Carsique
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.commandline.executor.service.executors;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters.ParameterValue;
import org.nuxeo.ecm.platform.commandline.executor.service.CommandLineDescriptor;
import org.nuxeo.ecm.platform.commandline.executor.service.EnvironmentDescriptor;

/**
 * Default implementation of the {@link Executor} interface. Use simple shell exec.
 */
public class ShellExecutor implements Executor {

    private static final Log log = LogFactory.getLog(ShellExecutor.class);

    @Deprecated
    @Override
    public ExecResult exec(CommandLineDescriptor cmdDesc, CmdParameters params) {
        return exec(cmdDesc, params, new EnvironmentDescriptor());
    }

    protected static final AtomicInteger PIPE_COUNT = new AtomicInteger();

    /** Used to split the contributed command, NOT the passed parameter values. */
    protected static final Pattern COMMAND_SPLIT = Pattern.compile("\"([^\"]*)\"|'([^']*)'|[^\\s]+");

    @Override
    public ExecResult exec(CommandLineDescriptor cmdDesc, CmdParameters params, EnvironmentDescriptor env) {
        String commandLine = cmdDesc.getCommand() + " " + String.join(" ", cmdDesc.getParametersString());
        try {
            if (log.isDebugEnabled()) {
                log.debug("Running system command: " + commandLine);
            }
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

        List<Process> processes = new LinkedList<>();
        List<Thread> pipes = new LinkedList<>();
        List<String> command = new LinkedList<>();
        Process process = null;
        for (Iterator<String> it = list.iterator(); it.hasNext();) {
            String word = it.next();
            boolean build;
            if (word.equals("|")) {
                build = true;
            } else {
                command.add(word);
                build = !it.hasNext();
            }
            if (!build) {
                continue;
            }
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            command = new LinkedList<>(); // reset for next loop
            processBuilder.directory(new File(env.getWorkingDirectory()));
            processBuilder.environment().putAll(env.getParameters());
            processBuilder.redirectErrorStream(true);
            Process newProcess = processBuilder.start();
            processes.add(newProcess);
            if (process == null) {
                // first process, nothing to input
                IOUtils.closeQuietly(newProcess.getOutputStream());
            } else {
                // pipe previous process output into new process input
                // needs a thread doing the piping because Java has no way to connect two children processes directly
                // except through a filesystem named pipe but that can't be created in a portable manner
                Thread pipe = pipe(process.getInputStream(), newProcess.getOutputStream());
                pipes.add(pipe);
            }
            process = newProcess;
        }

        // get result from last process
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        List<String> output = new ArrayList<>();
        while ((line = reader.readLine()) != null) {
            output.add(line);
        }
        reader.close();

        // wait for all processes, get first non-0 exit status
        int returnCode = 0;
        for (Process p : processes) {
            try {
                int exitCode = p.waitFor();
                if (returnCode == 0) {
                    returnCode = exitCode;
                }
            } catch (InterruptedException e) {
                ExceptionUtils.checkInterrupt(e);
            }
        }

        // wait for all pipes
        for (Thread t : pipes) {
            try {
                t.join();
            } catch (InterruptedException e) {
                ExceptionUtils.checkInterrupt(e);
            }
        }

        return new ExecResult(null, output, 0, returnCode);
    }

    /**
     * Returns a started daemon thread piping bytes from the InputStream to the OutputStream.
     * <p>
     * The streams are both closed when the copy is finished.
     *
     * @since 7.10
     */
    public static Thread pipe(InputStream in, OutputStream out) {
        Runnable run = new Runnable() {
            @Override
            public void run() {
                try {
                    IOUtils.copy(in, out);
                    out.flush();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    IOUtils.closeQuietly(in);
                    IOUtils.closeQuietly(out);
                }
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

}
