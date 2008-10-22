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

package org.nuxeo.ecm.webengine.model;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class Template {

    protected static final Map<String,String> mediaTypeNames = new HashMap<String, String>();
    static {
        mediaTypeNames.put(MediaType.APPLICATION_ATOM_XML, "atom");
        mediaTypeNames.put(MediaType.APPLICATION_JSON, "json");
    }

    protected String name = "view";
    protected String ext = "ftl";
    protected MediaType mediaType;
    protected final Resource resource;
    protected Map<String, Object> args;
    protected ScriptFile script;
    protected WebContext ctx;

    public Template(WebContext ctx, Resource resource, ScriptFile script, Map<String, Object> args) {
        this.ctx = ctx;
        this.resource = resource;
        this.script = script;
        this.args = args;
        if (this.ctx == null && this.resource != null) {
            this.ctx = this.resource.getContext();
        }
        mediaType = ctx.getHttpHeaders().getMediaType();
    }

    public Template(WebContext ctx) {
        this(ctx, null, null, null);
    }

    public Template(WebContext ctx, Map<String, Object> args) {
        this(ctx, null, null, args);
    }

    public Template(WebContext ctx, ScriptFile script) {
        this(ctx, null, script, null);
    }

    public Template(WebContext ctx, ScriptFile script, Map<String, Object> args) {
        this(ctx, null, script, args);
    }

    public Template(Resource resource) {
        this(resource.getContext(), resource, null, null);
    }

    public Template(Resource resource, Map<String, Object> args) {
        this(resource.getContext(), resource, null, null);
    }

    public Template(Resource resource, ScriptFile script, Map<String,Object> args) {
        this(resource.getContext(), resource, script, args);
    }

    public Template(Resource resource, ScriptFile script) {
        this(resource.getContext(), resource, script, null);
    }

    public Template mediaType(MediaType mediaType) {
        this.mediaType = mediaType;
        return this;
    }

    public MediaType mediaType() {
        return mediaType;
    }

    public Template arg(String key, Object value) {
        if (args == null) {
            args = new HashMap<String, Object>();
        }
        args.put(key, value);
        return this;
    }

    public Template args(Map<String,Object> args) {
        this.args = args;
        return this;
    }

    public Map<String,Object> args() {
        return args;
    }

    public Resource resource() {
        return resource;
    }

    public Template name(String name) {
        this.name = name;
       return this;
    }

    public Template extension(String ext) {
        this.ext = ext;
        return this;
    }

    public Template fileName(String fileName) throws WebException {
        if (resource != null) {
            script = resource.getType().getTemplate(fileName);
        } else {
            script = ctx.getModule().getFile(fileName);
        }
        return this;
    }

    public Template script(ScriptFile script) {
        this.script = script;
        return this;
    }

    public ScriptFile script() {
        return script;
    }

    public boolean isResolved() {
        return script != null;
    }

    public Template resolve() {
        if (script == null) {
            StringBuilder fileName = new StringBuilder();
            if (mediaType != null) {
                StringBuilder buf = new StringBuilder();
                buf.append(mediaType.getType().toLowerCase()).append('/').append(mediaType.getSubtype().toLowerCase());
                String type = mediaTypeNames.get(buf.toString());
                if (type != null) {
                    fileName.append(name).append('-').append(type).append('.').append(ext);
                } else {
                    fileName.append(name).append('.').append(ext);
                }
            } else {
                fileName.append(name).append('.').append(ext);
            }
            if (resource != null) {
                script = resource.getType().getTemplate(fileName.toString());
            } else {
                script = ctx.getModule().getFile(fileName.toString());
            }
        }
        return this;
    }

    public void render(OutputStream out) throws WebException {
        Writer w = new OutputStreamWriter(out);
        try {
            ctx.render(resolve().script(), args, w);
            w.flush();
        } catch (Exception e) {
            WebException.wrap("Failed to write response", e);
        }
    }

}
