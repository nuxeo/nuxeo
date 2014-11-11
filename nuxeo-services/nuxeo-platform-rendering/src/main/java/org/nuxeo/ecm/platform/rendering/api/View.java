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
 */
package org.nuxeo.ecm.platform.rendering.api;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class View {

    protected RenderingEngine renderingEngine;

    protected String name;

    protected Object object;

    protected Map<String, Object> args;

    public View(RenderingEngine renderingEngine, String name) {
        this (renderingEngine, name, null);
    }

    public View(RenderingEngine renderingEngine, String name, Object object) {
        this.renderingEngine = renderingEngine;
        this.name = name;
        this.args = new HashMap<String, Object>();
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

    public View args(Map<String,Object> args) {
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
        } catch (Exception e) {
            throw new RenderingException(e);
        }
    }

    public void render(OutputStream out, Charset charset) throws RenderingException {
        render(new OutputStreamWriter(out, charset));
    }

    public void render(Writer writer) throws RenderingException {
        try {
            renderingEngine.render(name, args, writer);
        } finally {
            try {
                writer.flush();
            } catch (Exception e) {
                throw new RenderingException(e);
            }
        }
    }

    @Override
    public String toString() {
        return object != null ? object.getClass().getName()+"#"+name : name;
    }

}
