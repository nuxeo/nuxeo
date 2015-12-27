/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     vpasquier <vpasquier@nuxeo.com>
 *     slacoin <slacoin@nuxeo.com>
 */
package org.nuxeo.ecm.automation.core.trace;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

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
        printLine(System.getProperty("line.separator") + System.getProperty("line.separator") + "****** " + heading
                + " ******");
    }

    public void print(Trace trace) throws IOException {
        StringBuilder sb = new StringBuilder();
        printHeading("chain");
        if (trace.error != null) {
            sb.append(System.getProperty("line.separator"));
            if (trace.getParent() != null) {
                sb.append("Parent Chain ID: ");
                sb.append(trace.getParent().getChainId());
                sb.append(System.getProperty("line.separator"));
            }
            sb.append("Name: ");
            sb.append(trace.getChain().getId());
            if (trace.getChain().getAliases() != null && trace.getChain().getAliases().length > 0) {
                sb.append(System.getProperty("line.separator"));
                sb.append("Aliases: ");
                sb.append(Arrays.toString(trace.getChain().getAliases()));
            }
            sb.append(System.getProperty("line.separator"));
            sb.append("Exception: ");
            sb.append(trace.error.getClass().getSimpleName());
            sb.append(System.getProperty("line.separator"));
            sb.append("Caught error: ");
            sb.append(trace.error.getMessage());
            sb.append(System.getProperty("line.separator"));
            sb.append("Caused by: ");
            sb.append(trace.error.getCause());
            printLine(sb.toString());
        } else {
            sb.append(System.getProperty("line.separator"));
            if (trace.getParent() != null) {
                sb.append("Parent Chain ID: ");
                sb.append(trace.getParent().getChainId());
                sb.append(System.getProperty("line.separator"));
            }
            sb.append("Name: ");
            sb.append(trace.getChain().getId());
            sb.append(System.getProperty("line.separator"));
            sb.append("Produced output type: ");
            sb.append(trace.output == null ? "Void" : trace.output.getClass().getSimpleName());
            printLine(sb.toString());
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(System.getProperty("line.separator"));
        stringBuilder.append("****** Hierarchy calls ******");
        stringBuilder.append(System.getProperty("line.separator"));
        displayOperationTreeCalls(trace.operations, stringBuilder);
        printLine(stringBuilder.toString());
        print(trace.operations);
        writer.flush();
    }

    public void print(List<Call> calls) throws IOException {
        for (Call call : calls) {
            print(call);
        }
    }

    public void print(Call call) throws IOException {
        printCall(call);
    }

    public void printCall(Call call) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append(System.getProperty("line.separator"));
            sb.append(System.getProperty("line.separator"));
            sb.append("****** " + call.getType().getId() + " ******");
            sb.append(System.getProperty("line.separator"));
            sb.append("Chain ID: ");
            sb.append(call.getChainId());
            if (call.getAliases() != null) {
                sb.append(System.getProperty("line.separator"));
                sb.append("Chain Aliases: ");
                sb.append(call.getAliases());
            }
            sb.append(System.getProperty("line.separator"));
            sb.append("Class: ");
            sb.append(call.getType().getType().getSimpleName());
            sb.append(System.getProperty("line.separator"));
            sb.append("Method: '");
            sb.append(call.getMethod().getMethod().getName());
            sb.append("' | Input Type: ");
            sb.append(call.getMethod().getConsume());
            sb.append(" | Output Type: ");
            sb.append(call.getMethod().getProduce());
            sb.append(System.getProperty("line.separator"));
            sb.append("Input: ");
            sb.append(call.getInput());
            if (!call.getParmeters().isEmpty()) {
                sb.append(System.getProperty("line.separator"));
                sb.append("Parameters ");
                for (String parameter : call.getParmeters().keySet()) {
                    sb.append(" | ");
                    sb.append("Name: ");
                    sb.append(parameter);
                    sb.append(", Value: ");
                    Object value = call.getParmeters().get(parameter);
                    if (value instanceof Call.ExpressionParameter) {
                        value = String.format("Expr:(id=%s | value=%s)",
                                ((Call.ExpressionParameter) call.getParmeters().get(parameter)).getParameterId(),
                                ((Call.ExpressionParameter) call.getParmeters().get(parameter)).getParameterValue());
                    }
                    sb.append(value);
                }
            }
            if (!call.getVariables().isEmpty()) {
                sb.append(System.getProperty("line.separator"));
                sb.append("Context Variables");
                for (String keyVariable : call.getVariables().keySet()) {
                    sb.append(" | ");
                    sb.append("Key: ");
                    sb.append(keyVariable);
                    sb.append(", Value: ");
                    Object variable = call.getVariables().get(keyVariable);
                    if (variable instanceof Calendar) {
                        sb.append(((Calendar) variable).getTime());
                    } else {
                        sb.append(variable);
                    }
                }
            }
            printLine(sb.toString());
            sb = new StringBuilder();
            if (!call.getNested().isEmpty()) {
                sb.append(System.getProperty("line.separator"));
                printHeading("start sub chain");
                for (Trace trace : call.getNested()) {
                    print(trace);
                }
                sb.append(System.getProperty("line.separator"));
                printHeading("end sub chain");
            }
            printLine(sb.toString());
        } catch (IOException e) {
            log.error("Nuxeo TracePrinter cannot write traces output", e);
        }
    }

    public void litePrint(Trace trace) throws IOException {
        StringBuilder sb = new StringBuilder();
        printHeading("chain");
        if (trace.error != null) {
            sb.append(System.getProperty("line.separator"));
            if (trace.getParent() != null) {
                sb.append("Parent Chain ID: ");
                sb.append(trace.getParent().getChainId());
                sb.append(System.getProperty("line.separator"));
            }
            sb.append("Name: ");
            sb.append(trace.getChain().getId());
            if (trace.getChain().getAliases() != null && trace.getChain().getAliases().length > 0) {
                sb.append(System.getProperty("line.separator"));
                sb.append("Aliases: ");
                sb.append(Arrays.toString(trace.getChain().getAliases()));
            }
            sb.append(System.getProperty("line.separator"));
            sb.append("Exception: ");
            sb.append(trace.error.getClass().getSimpleName());
            sb.append(System.getProperty("line.separator"));
            sb.append("Caught error: ");
            sb.append(trace.error.getMessage());
            sb.append(System.getProperty("line.separator"));
            sb.append("Caused by: ");
            sb.append(trace.error.getCause());
        }
        sb.append(System.getProperty("line.separator"));
        sb.append("****** Hierarchy calls ******");
        printLine(sb.toString());
        litePrintCall(trace.operations);
        writer.flush();
    }

    public void litePrintCall(List<Call> calls) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(System.getProperty("line.separator"));
        try {
            displayOperationTreeCalls(calls, stringBuilder);
            printLine(stringBuilder.toString());
            stringBuilder = new StringBuilder();
            for (Call call : calls) {
                if (!call.getNested().isEmpty()) {
                    stringBuilder.append(System.getProperty("line.separator"));
                    printHeading("start sub chain");
                    for (Trace trace : call.getNested()) {
                        litePrint(trace);
                    }
                    stringBuilder.append(System.getProperty("line.separator"));
                    printHeading("end sub chain");
                }
            }
            printLine(stringBuilder.toString());
        } catch (IOException e) {
            log.error("Nuxeo TracePrinter cannot write traces output", e);
        }
    }

    private void displayOperationTreeCalls(List<Call> calls, StringBuilder stringBuilder) {
        String tabs = "\t";
        for (Call call : calls) {
            stringBuilder.append(tabs);
            stringBuilder.append(call.getType().getType().getName());
            stringBuilder.append(System.getProperty("line.separator"));
            tabs += "\t";
        }
    }

}
