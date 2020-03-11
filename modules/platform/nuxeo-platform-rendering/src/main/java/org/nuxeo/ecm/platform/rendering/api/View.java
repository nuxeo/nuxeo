/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.platform.rendering.api;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class View {

    protected RenderingEngine renderingEngine;

    protected String name;

    protected Object object;

    protected Map<String, Object> args;

    public View(RenderingEngine renderingEngine, String name) {
        this(renderingEngine, name, null);
    }

    public View(RenderingEngine renderingEngine, String name, Object object) {
        this.renderingEngine = renderingEngine;
        this.name = name;
        this.args = new HashMap<>();
        forObject(object);
    }

    public String getName() {
        return name;
    }

    /**
     * @return the object
     */
    public Object getObject() {
        return object;
    }

    /**
     * @return the renderingEngine
     */
    public RenderingEngine getRenderingEngine() {
        return renderingEngine;
    }

    public View forObject(Object object) {
        this.object = object;
        args.put("This", object);
        return this;
    }

    public View arg(String key, Object value) {
        args.put(key, value);
        return this;
    }

    public View args(Map<String, Object> args) {
        this.args.putAll(args);
        return this;
    }

    public void render(OutputStream out) throws RenderingException {
        render(new OutputStreamWriter(out));
    }

    public void render(OutputStream out, String charset) throws RenderingException {
        try {
            render(new OutputStreamWriter(out, charset));
        } catch (RenderingException e) {
            throw e;
        } catch (IOException e) {
            throw new RenderingException(e);
        }
    }

    public void render(OutputStream out, Charset charset) throws RenderingException {
        render(new OutputStreamWriter(out, charset));
    }

    public void render(Writer writer) throws RenderingException {
        renderingEngine.render(name, args, writer);
        try {
            writer.flush();
        } catch (IOException e) {
            throw new RenderingException(e);
        }
    }

    @Override
    public String toString() {
        return object != null ? object.getClass().getName() + "#" + name : name;
    }

}
