/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     "Stephane Lacoin (aka matic) <slacoin@nuxeo.org>"
 */
package org.nuxeo.ecm.core.persistence;

/**
 * @author "Stephane Lacoin (aka matic) <slacoin@nuxeo.org>"
 *
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
