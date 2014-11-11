/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.webengine.jaxrs.views;

import java.io.File;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class View {

    public static URL resolveFile(File file) throws ViewNotFoundException {
        if (!file.isFile()) {
            throw new ViewNotFoundException(null, null, file.getAbsolutePath());
        }
        try {
            return file.toURI().toURL();
        } catch (Exception e) {
            throw new ViewNotFoundException(e, null, file.getAbsolutePath());
        }
    }

    public static URL resolveResource(Object owner, String name) throws ViewNotFoundException {
        URL url = owner.getClass().getResource(name);
        if (url == null) {
            throw new ViewNotFoundException(null, owner, name);
        }
        return url;
    }

    protected Object owner;

    protected final URL url;

    protected final Map<String, Object> vars;


    protected View(Object owner, String name) {
        this(owner, resolveResource(owner, name));
    }

    protected View(File file) {
        this(null, file);
    }

    protected View(Object owner, File file) {
        this(owner, resolveFile(file));
    }

    protected View(URL url) {
        this(null, url);
    }

    protected View(Object owner, URL url) {
        vars = new HashMap<String, Object>();
        this.url = url;
        if (owner != null) {
            forObject(owner);
        }
    }

    public View forObject(Object owner) {
        this.owner = owner;
        vars.put("This", owner);
        return this;
    }

    public URL getUrl() {
        return url;
    }

    public Object getOwner() {
        return owner;
    }

    public View arg(String key, Object value) {
        vars.put(key, value);
        return this;
    }

    public abstract void render(Writer writer) throws Exception;

    public abstract void render(OutputStream out) throws Exception;

}
