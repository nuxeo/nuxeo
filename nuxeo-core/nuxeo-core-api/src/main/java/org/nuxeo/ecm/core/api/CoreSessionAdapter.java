/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.runtime.api.ServiceAdapter;
import org.nuxeo.runtime.api.ServiceDescriptor;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class CoreSessionAdapter implements ServiceAdapter {

    private static final long serialVersionUID = 378521206345005762L;

    @Override
    public Object adapt(ServiceDescriptor svc, Object service) throws Exception {
        Map<String, Serializable> ctx = new HashMap<String, Serializable>();
        CoreSession session = (CoreSession) service;
        session.connect(svc.getName(), ctx);
        return session;
    }

}
