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

package org.nuxeo.ecm.webengine.util;

import net.sf.json.JSONNull;
import net.sf.json.JSONSerializer;

import org.nuxeo.ecm.webengine.WebContext;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ClientContext {
    public final static Object NULL = new Object();

    protected WebContext ctx;
    protected String name;
    protected Object value;


    public static ClientContext getActiveContext(WebContext ctx) {
        return new ClientContext(ctx, (String)ctx.getProperty("cctx", "default"));
    }

    public static ClientContext getContext(WebContext ctx, String contextName) {
        return new ClientContext(ctx, contextName);
    }

    public static String url(String url, String context) {
        StringBuilder buf = new StringBuilder(url.length()+32);
        buf.append(url);
        if (url.indexOf('?') > -1) {
            buf.append("&cctx=").append(context);
        } else {
            buf.append("?cctx=").append(context);
        }
        return buf.toString();
    }

    public ClientContext(WebContext ctx, String contextName) {
        this.ctx = ctx;
        this.name = contextName;
    }

    public boolean isDefault() {
        return "default".equals(name);
    }

    public Object getValue() {
        if (value == null) {
            String str = ctx.getCookie("cctx."+name);
            if (str == null) {
                value = NULL;
                return null;
            } else if (str.length() > 1) {
                char c = str.charAt(0);
                if (c == '{' || c == '[') {
                    value = JSONSerializer.toJSON(str);
                } else {
                    value = str;
                }
            }
        }
        return value;
    }

    public Object getValue(Object defaultValue) {
        Object val = getValue();
        return val == null ? defaultValue : val;
    }

    public void setValue(Object value) {
        if (value == null) {
            value = NULL;
        }
        if (value.getClass() == String.class || value instanceof Number) {
            this.value = value;
        } else if (value instanceof Number || value instanceof Boolean || value instanceof CharSequence) {
            this.value = value.toString();
        } else {
            this.value = JSONSerializer.toJSON(value);
        }
    }

    public void save() {
        if (value != null) {
            if (value == null) {
                ctx.setCookie("cctx."+name, null);
            } else {
                ctx.setCookie("cctx."+name, value.toString());
            }
        }
    }

}
