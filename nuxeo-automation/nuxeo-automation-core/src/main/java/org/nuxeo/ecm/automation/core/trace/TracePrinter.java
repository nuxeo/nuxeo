/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     vpasquier <vpasquier@nuxeo.com>
 *     slacoin <slacoin@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.trace;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationException;

/**
 * @since 5.7.3
 */
public class TracePrinter {

    private static final Log log = LogFactory.getLog(TracePrinter.class);

    protected final BufferedWriter writer;

    protected String preamble = "";

    public TracePrinter(Writer writer) {
        this.writer = new BufferedWriter(writer);
    }

    public TracePrinter(OutputStream out) {
        this(new OutputStreamWriter(out));
    }

    protected void printLine(String line) throws IOException {
        writer.write(preamble + line);
    }

    protected void printHeading(String heading) throws IOException {
        printLine(System.getProperty("line.separator") + "****** " + heading
                + " ******");
    }

    public void print(Trace trace) throws IOException {
        StringBuilder sb = new StringBuilder();
        printHeading("chain");
        if (trace.error != null) {
            sb.append(System.getProperty("line.separator"));
            sb.append("Name: ");
            sb.append(trace.getChain().getId());
            sb.append(System.getProperty("line.separator"));
            print(trace.operations);
            sb.append(System.getProperty("line.separator"));
            sb.append("Exception: ");
            sb.append(trace.error.getClass().getSimpleName());
            sb.append(System.getProperty("line.separator"));
            sb.append("Caught error: ");
            sb.append(trace.error.getMessage());
            sb.append(System.getProperty("line.separator"));
            sb.append("Caused by: ");
            sb.append(trace.error.getCause());
            sb.append(System.getProperty("line.separator"));
            if (trace.error.getStackTrace().length != 0) {
                sb.append("StackTrace: ");
                sb.append(trace.error.getCause());
                sb.append(System.getProperty("line.separator"));
            }
            printLine(sb.toString());
            printError(trace.error);
        } else {
            printLine("produced output of type "
                    + trace.output.getClass().getSimpleName());
            // printObject((OperationType) trace.output);
        }
        writer.flush();
    }

    public void printError(OperationException error) throws IOException {

    }

    public void print(List<Call> calls) throws IOException {
        for (Call call : calls) {
            print(call);
        }
    }

    public void print(Call call) throws IOException {
        printHeading("operation");
        printCall(call);
    }

    public void printCall(Call call) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(System.getProperty("line.separator"));
            sb.append("****** " + call.getType().getId() + " ******");
            sb.append(System.getProperty("line.separator"));
            sb.append("Class: ");
            sb.append(call.getClass().getSimpleName());
            sb.append(System.getProperty("line.separator"));
            sb.append("Method: ");
            sb.append(call.getMethod().getClass().getSimpleName());
            sb.append(System.getProperty("line.separator"));
            sb.append("Input: ");
            sb.append(call.getInput());
            if (!call.getParmeters().isEmpty()) {
                sb.append(System.getProperty("line.separator"));
                sb.append("Parameters: ");
                for (Object parameter : call.getParmeters().values()) {
                    sb.append(parameter.toString());
                }
            }
            if (!call.getVariables().isEmpty()) {
                sb.append(System.getProperty("line.separator"));
                sb.append("Runtime Variables");
                for (String keyVariable : call.getVariables().keySet()) {
                    sb.append("Key: ");
                    sb.append(keyVariable);
                    sb.append(System.getProperty("line.separator"));
                    sb.append("Value: ");
                    sb.append(call.getVariables().get(keyVariable));
                    sb.append(System.getProperty("line.separator"));
                }
            }
            printLine(sb.toString());
        } catch (IOException e) {
            log.error(e);
        }
    }
}
