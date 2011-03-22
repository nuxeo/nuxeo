/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Stephane Lacoin, Julien Carsique
 *
 * $Id$
 */

package org.nuxeo.runtime.transaction;

/**
 * Reified checked errors caught while operating the transaction. The error is
 * logged but the error condition is re-throwed as a runtime, enabling
 * transaction helper callers being able to be aware about the error..
 *
 * @author "Stephane Lacoin at Nuxeo (aka matic)"
 *
 */
public class TransactionRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TransactionRuntimeException(String msg, Throwable error) {
        super(msg, error);
    }

    public TransactionRuntimeException(String msg) {
        super(msg);
    }

}
