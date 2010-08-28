/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
