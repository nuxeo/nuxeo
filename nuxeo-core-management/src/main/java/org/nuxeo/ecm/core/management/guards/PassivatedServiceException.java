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
 *     "Stephane Lacoin [aka matic] <slacoin at nuxeo.com>"
 */
package org.nuxeo.ecm.core.management.guards;

import java.lang.reflect.Method;

/**
 * That exception is throwed by the {@link GuardedServiceHandler} whenever a call is made
 * onto a service guarded while the server is passivated.
 *
 * @author "Stephane Lacoin [aka matic] <slacoin at nuxeo.com>"
 *
 */
public class PassivatedServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public final String statusId;
    public final Method method;

    protected PassivatedServiceException(Method m, String statusId) {
        super(m.getDeclaringClass().getCanonicalName().concat(".").concat(m.getName()).concat(" cannot being accessed while status ").concat(statusId).concat(" is passive"));
        this.statusId = statusId;
        this.method = m;
    }

}
