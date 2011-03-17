/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation;

/**
 * The base exception of the operation service.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class OperationException extends Exception {

    private static final long serialVersionUID = 1L;

    protected boolean rollback = true;

    public OperationException(String message) {
        super(message);
    }

    public OperationException(Throwable cause) {
        super(cause);
    }

    public OperationException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Whether this exception should rollback the current transaction.
     * The default is true if not explicitly set by calling {@link #setNoRollback()}.
     * @return
     */
    public boolean isRollback() {
        return rollback;
    }

    public OperationException setNoRollback() {
        this.rollback = false;
        return this;
    }
}
