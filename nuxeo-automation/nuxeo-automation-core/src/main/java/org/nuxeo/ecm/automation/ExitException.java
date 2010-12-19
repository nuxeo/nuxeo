/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation;

/**
 * Throw it from an operation to interrupt a chain execution.
 * The chain terminates silently (without throwing an exception) and the <code>output</code>
 * object is returned as the chain output.
 * <p>
 * Also, you can set the <code>rollback</code> argument to true to rollback the current transaction.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@SuppressWarnings("serial")
public class ExitException extends OperationException {

    protected Object output;

    public ExitException() {
        this (null, false);
    }

    public ExitException(Object output) {
        this (output, false);
    }

    public ExitException(Object output, boolean rollback) {
        super ("Chain Interrupted");
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
