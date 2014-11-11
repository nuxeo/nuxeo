/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     "Stephane Lacoin [aka matic] <slacoin at nuxeo.com>"
 */
package org.nuxeo.ecm.core.management.guards;

import java.lang.reflect.Method;

/**
 * That exception is thrown by the {@link GuardedServiceHandler} whenever a call is made
 * onto a service guarded while the server is passivated.
 *
 * @author "Stephane Lacoin [aka matic] <slacoin at nuxeo.com>"
 */
public class PassivatedServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public final String statusId;
    public final Method method;

    protected PassivatedServiceException(Method m, String statusId) {
        super(m.getDeclaringClass().getCanonicalName() + "." + m.getName()
                + " cannot being accessed while status " + statusId + " is passive");
        this.statusId = statusId;
        this.method = m;
    }

}
