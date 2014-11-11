/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     matic
 */
package org.nuxeo.runtime.transaction;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.api.AnnotatedServiceProvider;

/**
 * Allocate transacted invocation handlers and return proxies if
 * service is annotated with Transacted annotations. The provider is to be
 * lazy installed by the client module using the install method.
 *
 * @author matic
 *
 */
public class TransactedServiceProvider extends AnnotatedServiceProvider {

    protected static final Log log = LogFactory.getLog(TransactedServiceProvider.class);

    public static final TransactedServiceProvider INSTANCE = new TransactedServiceProvider();

    public static void install() {
        INSTANCE.installSelf();
    }

    @Override
    protected <T> T newProxy(T object, Class<T> clazz) {
        return  TransactedInstanceHandler.newProxy(object, clazz);
    }

    @Override
    protected Class<Transacted> annotationClass() {
        return Transacted.class;
    }


}
