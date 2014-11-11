/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.model.impl;

import java.net.URL;

import org.nuxeo.common.xmap.Context;
import org.nuxeo.runtime.model.RuntimeContext;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
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
        return ctx.loadClass(className);
    }

    @Override
    public URL getResource(String name) {
        return ctx.getResource(name);
    }

}
