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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.config;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

import org.nuxeo.common.xmap.XMap;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.model.impl.XMapContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class XConfiguration {

    protected XMap xmap;
    protected XMapContext ctx;

    public XConfiguration(RuntimeContext ctx, Class<?> ... xobjs) {
        this.ctx = new XMapContext(ctx);
        this.xmap = new XMap();
        if (xobjs != null) {
            for (int i=0; i<xobjs.length; i++) {
                xmap.register(xobjs[i]);
            }
        }
    }

    public XMap getXMap() {
        return xmap;
    }

    public XMapContext getXMapContext() {
        return ctx;
    }

    public Object load(File file) throws Exception {
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        try {
            return load(in);
        } finally {
            in.close();
        }
    }

    public Object loadAll(File file) throws Exception {
        InputStream in = new BufferedInputStream(new FileInputStream(file));
        try {
            return loadAll(in);
        } finally {
            in.close();
        }
    }

    public Object load(URL url) throws Exception {
        InputStream in = new BufferedInputStream(url.openStream());
        try {
            return load(in);
        } finally {
            in.close();
        }
    }

    public Object loadAll(URL url) throws Exception {
        InputStream in = new BufferedInputStream(url.openStream());
        try {
            return loadAll(in);
        } finally {
            in.close();
        }
    }
    public Object load(InputStream in) throws Exception {
        return xmap.load(in);
    }

    public Object[] loadAll(InputStream in) throws Exception {
        return xmap.loadAll(in);
    }

}
