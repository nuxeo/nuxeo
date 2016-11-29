/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.model;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.SocketException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.ExceptionUtils;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.scripting.ScriptFile;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Template {

    private static final Log log = LogFactory.getLog(Template.class);

    protected final Resource resource;

    protected Map<String, Object> args;

    protected ScriptFile script;

    protected WebContext ctx;

    protected Template(WebContext ctx, Resource resource, ScriptFile script) {
        this.ctx = ctx;
        this.resource = resource;
        this.script = script;
        if (this.ctx == null && this.resource != null) {
            this.ctx = this.resource.getContext();
        }
    }

    public Template(WebContext ctx, String fileName) {
        this(ctx, null, null);
        resolve(fileName);
    }

    public Template(Resource resource, String fileName) {
        this(resource.getContext(), resource, null);
        resolve(fileName);
    }

    public Template(WebContext ctx, ScriptFile script) {
        this(ctx, null, script);
    }

    public Template(Resource resource, ScriptFile script) {
        this(resource.getContext(), resource, script);
    }

    public Template arg(String key, Object value) {
        if (args == null) {
            args = new HashMap<String, Object>();
        }
        args.put(key, value);
        return this;
    }

    public Template args(Map<String, Object> args) {
        this.args = args;
        return this;
    }

    public Map<String, Object> args() {
        return args;
    }

    public Resource resource() {
        return resource;
    }

    protected void resolve(String fileName) {
        if (resource != null) {
            script = resource.getType().getView(ctx.getModule(), fileName);
        } else {
            script = ctx.getModule().getFile(fileName);
        }
    }

    public ScriptFile script() {
        return script;
    }

    public void render(OutputStream out) throws WebException {
        Writer w;
        try {
            w = new OutputStreamWriter(out, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw WebException.wrap("Failed to create output stream: unsupported encoding", e);
        }
        ctx.render(script(), args, w);
        try {
            w.flush();
        } catch (IOException e) {
            if (ExceptionUtils.getRootCause(e) instanceof SocketException) {
                log.debug("Output socket closed: failed to flush response");
            } else {
                throw WebException.wrap("Failed to flush response", e);
            }
        }
    }

    public String render() {
        StringWriter w = new StringWriter();
        ctx.render(script(), args, w);
        w.flush();
        return w.getBuffer().toString();
    }

}
