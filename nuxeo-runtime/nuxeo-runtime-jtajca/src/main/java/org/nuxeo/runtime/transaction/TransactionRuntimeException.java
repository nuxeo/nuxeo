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
 *     Stephane Lacoin, Julien Carsique
 *
 * $Id$
 */

package org.nuxeo.runtime.transaction;

/**
 * Reified checked errors caught while operating the transaction. The error is logged but the error condition is
 * re-throwed as a runtime, enabling transaction helper callers being able to be aware about the error..
 *
 * @author "Stephane Lacoin at Nuxeo (aka matic)"
 */
public class TransactionRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TransactionRuntimeException(String msg, Throwable error) {
        super(msg, error);
    }

    public TransactionRuntimeException(String msg) {
        super(msg);
    }

    public TransactionRuntimeException(Throwable cause) {
        super(cause);
    }

}
