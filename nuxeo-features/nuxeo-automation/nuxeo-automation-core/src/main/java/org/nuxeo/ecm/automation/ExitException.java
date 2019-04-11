/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation;

/**
 * Throw it from an operation to interrupt a chain execution. The chain terminates silently (without throwing an
 * exception) and the <code>output</code> object is returned as the chain output.
 * <p>
 * Also, you can set the <code>rollback</code> argument to true to rollback the current transaction.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@SuppressWarnings("serial")
public class ExitException extends OperationException {

    private static final long serialVersionUID = 1L;
    protected Object output;

    public ExitException() {
        this(null, false);
    }

    public ExitException(Object output) {
        this(output, false);
    }

    public ExitException(Object output, boolean rollback) {
        super("Chain Interrupted");
        this.output = output;
        this.rollback = rollback;
    }

    public ExitException setOutput(Object output) {
        this.output = output;
        return this;
    }

    public Object getOutput() {
        return output;
    }

}
