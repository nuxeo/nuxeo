/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     "Stephane Lacoin (aka matic) <slacoin@nuxeo.org>"
 */
package org.nuxeo.ecm.core.persistence;

/**
 * @author "Stephane Lacoin (aka matic) <slacoin@nuxeo.org>"
 */
public class PersistenceError extends Error {

    private static final long serialVersionUID = 1L;

    protected PersistenceError(String message, Throwable cause) {
        super(message, cause);
    }

    public PersistenceError(String message) {
        super(message);
    }

    public static PersistenceError wrap(String message, Throwable cause) {
        return new PersistenceError(message, cause);
    }

}
