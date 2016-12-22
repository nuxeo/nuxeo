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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
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

    private static final String LF = System.getProperty("line.separator");

    protected final BufferedWriter writer;


    public TracePrinter(Writer writer) {
        this.writer = new BufferedWriter(writer);
    }

    public TracePrinter(OutputStream out) {
        this(new OutputStreamWriter(out));
    }

    protected void printHeading(String heading) throws IOException {
        writer.append(LF+ LF + "****** " + heading + " ******");
    }

    protected void printCalls(List<Call> calls) throws IOException {
        String tabs = "\t";
        for (Call call : calls) {
            writer.append(tabs);
            writer.append(call.getType().getType().getName());
            writer.append(LF);
            tabs += "\t";
        }
    }

    public void print(Trace trace) throws IOException {
        printHeading("chain");
        if (trace.error != null) {
            writer.append(LF);
            if (trace.getParent() != null) {
                writer.append("Parent Chain ID: ");
                writer.append(trace.getParent().getChainId());
                writer.append(LF);
            }
            writer.append("Name: ");
            writer.append(trace.getChain().getId());
            if (trace.getChain().getAliases() != null && trace.getChain().getAliases().length > 0) {
                writer.append(LF);
                writer.append("Aliases: ");
                writer.append(Arrays.toString(trace.getChain().getAliases()));
            }
            writer.append(LF);
            writer.append("Exception: ");
            writer.append(trace.error.getClass().getSimpleName());
            writer.append(LF);
            writer.append("Caught error: ");
            writer.append(trace.error.getMessage());
            writer.append(LF);
            writer.append("Caused by: ");
            writer.append(trace.error.toString());
        } else {
            writer.append(LF);
            if (trace.getParent() != null) {
                writer.append("Parent Chain ID: ");
                writer.append(trace.getParent().getChainId());
                writer.append(LF);
            }
            writer.append("Name: ");
            writer.append(trace.getChain().getId());
            writer.append(LF);
            writer.append("Produced output type: ");
            writer.append(trace.output == null ? "Void" : trace.output.getClass().getSimpleName());
        }
        writer.append(LF);
        writer.append("****** Hierarchy calls ******");
        writer.append(LF);
        printCalls(trace.calls);
        print(trace.calls);
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
            writer.append(LF);
            writer.append(LF);
            writer.append("****** " + call.getType().getId() + " ******");
            writer.append(LF);
            writer.append("Chain ID: ");
            writer.append(call.getChainId());
            if (call.getAliases() != null) {
                writer.append(LF);
                writer.append("Chain Aliases: ");
                writer.append(call.getAliases());
            }
            writer.append(LF);
            writer.append("Class: ");
            writer.append(call.getType().getType().getSimpleName());
            writer.append(LF);
            writer.append("Method: '");
            writer.append(call.getMethod().getMethod().getName());
            writer.append("' | Input Type: ");
            writer.append(call.getMethod().getConsume().getName());
            writer.append(" | Output Type: ");
            writer.append(call.getMethod().getProduce().getName());
            writer.append(LF);
            writer.append("Input: ");
            writer.append(call.getInput() == null ? "null" : call.getInput().toString());
            if (!call.getParameters().isEmpty()) {
                writer.append(LF);
                writer.append("Parameters ");
                for (String parameter : call.getParameters().keySet()) {
                    writer.append(" | ");
                    writer.append("Name: ");
                    writer.append(parameter);
                    writer.append(", Value: ");
                    Object value = call.getParameters().get(parameter);
                    if (value instanceof Call.ExpressionParameter) {
                        value = String.format("Expr:(id=%s | value=%s)",
                                ((Call.ExpressionParameter) call.getParameters().get(parameter)).getParameterId(),
                                ((Call.ExpressionParameter) call.getParameters().get(parameter)).getParameterValue());
                    }
                    writer.append(value.toString());
                }
            }
            if (!call.getVariables().isEmpty()) {
                writer.append(LF);
                writer.append("Context Variables");
                for (String keyVariable : call.getVariables().keySet()) {
                    writer.append(" | ");
                    writer.append("Key: ");
                    writer.append(keyVariable);
                    writer.append(", Value: ");
                    Object variable = call.getVariables().get(keyVariable);
                    if (variable instanceof Calendar) {
                        writer.append(((Calendar) variable).getTime().toString());
                    } else {
                        writer.append(variable == null ? "null" : variable.toString());
                    }
                }
            }
            if (!call.getNested().isEmpty()) {
                writer.append(LF);
                printHeading("start sub chain");
                for (Trace trace : call.getNested()) {
                    print(trace);
                }
                writer.append(LF);
                printHeading("end sub chain");
            }
        } catch (IOException e) {
            log.error("Nuxeo TracePrinter cannot write traces output", e);
        }
    }

    public void litePrint(Trace trace) throws IOException {
        printHeading("chain");
        writer.append(LF);
        if (trace.getParent() != null) {
            writer.append("Parent Chain ID: ");
            writer.append(trace.getParent().getChainId());
            writer.append(LF);
        }
        writer.append("Name: ");
        writer.append(trace.getChain().getId());
        if (trace.getChain().getAliases() != null && trace.getChain().getAliases().length > 0) {
            writer.append(LF);
            writer.append("Aliases: ");
            writer.append(Arrays.toString(trace.getChain().getAliases()));
        }
        if (trace.error != null) {
            writer.append(LF);
            writer.append("Exception: ");
            writer.append(trace.error.getClass().getSimpleName());
            writer.append(LF);
            writer.append("Caught error: ");
            writer.append(trace.error.getMessage());
            writer.append(LF);
            writer.append("Caused by: ");
            writer.append(trace.error.toString());
        }
        writer.append(LF);
        writer.append("****** Hierarchy calls ******");
        litePrintCall(trace.calls);
        writer.flush();
    }

    public void litePrintCall(List<Call> calls) throws IOException {
        writer.append(LF);
        try {
            printCalls(calls);
            for (Call call : calls) {
                if (!call.getNested().isEmpty()) {
                    writer.append(LF);
                    printHeading("start sub chain");
                    for (Trace trace : call.getNested()) {
                        litePrint(trace);
                    }
                    writer.append(LF);
                    printHeading("end sub chain");
                }
            }
        } catch (IOException e) {
            log.error("Nuxeo TracePrinter cannot write traces output", e);
        }
    }

    public static String print(Trace trace, boolean liteprint) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        TracePrinter printer = new TracePrinter(out);
        try {
            if (liteprint) {
                printer.litePrint(trace);
            } else {
                printer.print(trace);
            }
        } catch (IOException cause) {
            return "Cannot print automation trace of " + trace.chain.getId();
        }
        return new String(out.toByteArray(), Charset.forName("UTF-8"));
    }

}
