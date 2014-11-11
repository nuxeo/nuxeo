/*
 * Copyright (c) 2006-2013 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *     Bogdan Stefanescu
 *     Benjamin JALON
 */

package org.nuxeo.runtime.model.impl;

import java.net.URL;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.runtime.model.RuntimeContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class XMapContext extends Context {

    private static final long serialVersionUID = -7194560385886298218L;

    final RuntimeContext ctx;

    public XMapContext(RuntimeContext ctx) {
        this.ctx = ctx;
    }

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
        return Class.forName(className, true, ctx.getClassLoader());
    }

    @Override
    public URL getResource(String name) {
        return ctx.getResource(name);
    }

}
