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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7.3
 */
public class Trace {

    protected final Call parent;

    protected final OperationType chain;

    protected final OperationException error;

    protected final Object output;

    protected final List<Call> operations;

    Trace(Call parent, OperationType chain, List<Call> operations) {
        this.parent = parent;
        // If chain doesn't exist, this should be one operation call
        this.chain = chain != null ? chain : operations.get(0).getType();
        this.operations = new ArrayList<Call>(operations);
        output = null;
        error = null;
    }

    Trace(Call parent, OperationType chain, List<Call> calls, OperationException error) {
        this.parent = parent;
        // If chain doesn't exist, this should be one operation call
        this.chain = chain != null ? chain : calls.get(0).getType();
        operations = new ArrayList<Call>(calls);
        output = null;
        this.error = error;
    }

    Trace(Call parent, OperationType chain, List<Call> calls, Object output) {
        this.parent = parent;
        // If chain doesn't exist, this should be one operation call
        this.chain = chain != null ? chain : calls.get(0).getType();
        operations = new ArrayList<Call>(calls);
        this.output = output;
        error = null;
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

    public Object getOutput() {
        return output;
    }

    public List<Call> getCalls() {
        return operations;
    }

    public String getFormattedText() {
        TracerFactory tracerFactory = Framework.getLocalService(TracerFactory.class);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            if (tracerFactory.getRecordingState()) {
                new TracePrinter(out).print(this);
            } else {
                new TracePrinter(out).litePrint(this);
            }
        } catch (IOException e) {
            LogFactory.getLog(Trace.class).error("Cannot print trace of " + chain.getId(), e);
            return chain.getId();
        }
        return out.toString();
    }
}
