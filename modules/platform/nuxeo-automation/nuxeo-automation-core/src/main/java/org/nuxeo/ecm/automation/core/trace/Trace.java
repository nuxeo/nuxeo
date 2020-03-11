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

import static org.nuxeo.ecm.automation.core.Constants.LF;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;

/**
 * @since 5.7.3
 */
public class Trace {

    private static final Log log = LogFactory.getLog(TracerFactory.class);

    protected final Call parent;

    protected final OperationType chain;

    protected final List<Call> calls;

    protected final Object input;

    protected final Object output;

    protected final OperationException error;

    protected Trace(Call parent, OperationType chain, List<Call> calls, Object input, Object output,
            OperationException error) {
        this.parent = parent;
        this.chain = chain;
        this.calls = new ArrayList<>(calls);
        this.input = input;
        this.output = output;
        this.error = error;
    }

    public Call getParent() {
        return parent;
    }

    public OperationType getChain() {
        return chain;
    }

    public OperationException getError() {
        return error;
    }

    public Object getInput() {
        return input;
    }

    public Object getOutput() {
        return output;
    }

    public List<Call> getCalls() {
        return calls;
    }

    @Override
    public String toString() {
        return print(true);
    }

    protected void printHeading(String heading, BufferedWriter writer) throws IOException {
        writer.append(LF).append(LF).append("****** ").append(heading).append(" ******");
    }

    protected void litePrint(BufferedWriter writer) throws IOException {
        printHeading("chain", writer);
        writer.append(LF);
        if (getParent() != null) {
            writer.append("Parent Chain ID: ");
            writer.append(getParent().getChainId());
            writer.append(LF);
        }
        writer.append("Name: ");
        writer.append(getChain().getId());
        if (getChain().getAliases() != null && getChain().getAliases().length > 0) {
            writer.append(LF);
            writer.append("Aliases: ");
            writer.append(Arrays.toString(getChain().getAliases()));
        }
        if (error != null) {
            writer.append(LF);
            writer.append("Exception: ");
            writer.append(error.getClass().getSimpleName());
            writer.append(LF);
            writer.append("Caught error: ");
            writer.append(error.getMessage());
            writer.append(LF);
            writer.append("Caused by: ");
            writer.append(error.toString());
        }
        writer.append(LF);
        writer.append("****** Hierarchy calls ******");
        litePrintCall(calls, writer);
        writer.flush();
    }

    protected void litePrintCall(List<Call> calls, BufferedWriter writer) throws IOException {
        writer.append(LF);
        try {
            printCalls(calls, writer);
            for (Call call : calls) {
                if (!call.getNested().isEmpty()) {
                    writer.append(LF);
                    printHeading("start sub chain", writer);
                    for (Trace trace : call.getNested()) {
                        trace.litePrint(writer);
                    }
                    writer.append(LF);
                    printHeading("end sub chain", writer);
                }
            }
        } catch (IOException e) {
            log.error("Nuxeo TracePrinter cannot write traces output", e);
        }
    }

    protected void print(BufferedWriter writer) throws IOException {
        printHeading("chain", writer);
        if (error != null) {
            writer.append(LF);
            if (getParent() != null) {
                writer.append("Parent Chain ID: ");
                writer.append(getParent().getChainId());
                writer.append(LF);
            }
            writer.append("Name: ");
            writer.append(getChain().getId());
            if (getChain().getAliases() != null && getChain().getAliases().length > 0) {
                writer.append(LF);
                writer.append("Aliases: ");
                writer.append(Arrays.toString(getChain().getAliases()));
            }
            writer.append(LF);
            writer.append("Exception: ");
            writer.append(error.getClass().getSimpleName());
            writer.append(LF);
            writer.append("Caught error: ");
            writer.append(error.getMessage());
            writer.append(LF);
            writer.append("Caused by: ");
            writer.append(error.toString());
        } else {
            writer.append(LF);
            if (getParent() != null) {
                writer.append("Parent Chain ID: ");
                writer.append(getParent().getChainId());
                writer.append(LF);
            }
            writer.append("Name: ");
            writer.append(getChain().getId());
            writer.append(LF);
            writer.append("Produced output type: ");
            writer.append(output == null ? "Void" : output.getClass().getSimpleName());
        }
        writer.append(LF);
        writer.append("****** Hierarchy calls ******");
        writer.append(LF);
        printCalls(calls, writer);
        writer.flush();
    }

    protected void printCalls(List<Call> calls, BufferedWriter writer) throws IOException {
        String tabs = "\t";
        for (Call call : calls) {
            writer.append(tabs);
            call.print(writer);
            tabs += "\t";
        }
    }

    public String print(boolean litePrint) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
        try {
            if (litePrint) {
                litePrint(writer);
            } else {
                print(writer);
            }
        } catch (IOException cause) {
            return "Cannot print automation trace of " + chain.getId();
        }
        return new String(out.toByteArray(), Charset.forName("UTF-8"));
    }
}
