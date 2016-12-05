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

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.automation.OperationException;
import org.nuxeo.ecm.automation.OperationType;

/**
 * @since 5.7.3
 */
public class Trace {

    protected final Call parent;

    protected final OperationType chain;

    protected final List<Call> calls;

    protected final Object input;

    protected final Object output;

    protected final OperationException error;

    protected Trace(Call parent, OperationType chain, List<Call> calls, Object input, Object output, OperationException error) {
        this.parent = parent;
        this.chain = chain;
        this.calls = new ArrayList<Call>(calls);
        this.input = input;
        this.output = output;;
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
        return TracePrinter.print(this, true);
    }
}
