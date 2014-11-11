/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
        this.output = null;
        this.error = null;
    }

    Trace(Call parent, OperationType chain, List<Call> calls,
            OperationException error) {
        this.parent = parent;
        // If chain doesn't exist, this should be one operation call
        this.chain = chain != null ? chain : calls.get(0).getType();
        this.operations = new ArrayList<Call>(calls);
        this.output = null;
        this.error = error;
    }

    Trace(Call parent, OperationType chain, List<Call> calls, Object output) {
        this.parent = parent;
        // If chain doesn't exist, this should be one operation call
        this.chain = chain != null ? chain : calls.get(0).getType();
        this.operations = new ArrayList<Call>(calls);
        this.output = output;
        this.error = null;
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
            LogFactory.getLog(Trace.class).error(
                    "Cannot print trace of " + chain.getId(), e);
            return chain.getId();
        }
        return out.toString();
    }
}
