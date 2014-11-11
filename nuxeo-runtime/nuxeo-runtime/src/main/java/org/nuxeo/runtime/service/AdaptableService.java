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
 *
 * $Id$
 */

package org.nuxeo.runtime.service;

import java.lang.reflect.InvocationTargetException;

import org.nuxeo.runtime.model.Adaptable;
import org.nuxeo.runtime.service.proxy.MethodInvocation;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface AdaptableService extends Adaptable {

    boolean hasAdapter(Class<?> adapter);

    Object invokeAdapter(MethodInvocation invocation, Object[] args)
            throws NoSuchAdapterException, InvocationTargetException, IllegalAccessException;

}
