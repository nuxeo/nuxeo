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

package org.nuxeo.ecm.platform.rendering.fm;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.nuxeo.ecm.platform.rendering.api.EmptyContextView;
import org.nuxeo.ecm.platform.rendering.api.RenderingContext;
import org.nuxeo.ecm.platform.rendering.api.RenderingContextView;
import org.nuxeo.ecm.platform.rendering.api.RenderingEngine;
import org.nuxeo.ecm.platform.rendering.api.RenderingException;
import org.nuxeo.ecm.platform.rendering.api.RenderingTransformer;
import org.nuxeo.ecm.platform.rendering.api.ResourceLocator;
import org.nuxeo.ecm.platform.rendering.fm.adapters.DocumentObjectWrapper;
import org.nuxeo.ecm.platform.rendering.fm.adapters.RenderingContextModel;
import org.nuxeo.ecm.platform.rendering.fm.extensions.BlockDirective;
import org.nuxeo.ecm.platform.rendering.fm.extensions.BlockWriter;
import org.nuxeo.ecm.platform.rendering.fm.extensions.BlockWriterRegistry;
import org.nuxeo.ecm.platform.rendering.fm.extensions.DocRefMethod;
import org.nuxeo.ecm.platform.rendering.fm.extensions.ExtendsDirective;
import org.nuxeo.ecm.platform.rendering.fm.extensions.MessagesMethod;
import org.nuxeo.ecm.platform.rendering.fm.extensions.NewMethod;
import org.nuxeo.ecm.platform.rendering.fm.extensions.QueryMethod;
import org.nuxeo.ecm.platform.rendering.fm.extensions.SuperBlockDirective;
import org.nuxeo.ecm.platform.rendering.fm.extensions.TransformDirective;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.cache.URLTemplateLoader;
import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.ObjectWrapper;
import freemarker.template.Template;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class FreemarkerEngine implements RenderingEngine {

    public final static String ROOT_CTX_KEY = "NX_ROOT_CTX";

    protected Configuration cfg;
    protected ResourceLocator locator;
    // the wrapper is not a singleton since it contains some info about the engine instance
    // so we will have one wrapper per engine instance
    protected DocumentObjectWrapper wrapper;
    // an empty env provider by default
    protected RenderingContextView sharedView = EmptyContextView.INSTANCE;
    protected Map<String, RenderingTransformer> transformers = new HashMap<String, RenderingTransformer>();

    protected MessagesMethod messages = new MessagesMethod(null);

    public FreemarkerEngine() {
        this (null, null, (File[])null);
    }

    /**
     *
     */
    public FreemarkerEngine(Configuration cfg, ResourceLocator locator, File ... resourceDirs) {
        this.wrapper = new DocumentObjectWrapper(this);
        this.locator = locator;
        this.cfg = cfg == null ? new Configuration() : cfg;
        this.cfg.setWhitespaceStripping(true);
        this.cfg.setLocalizedLookup(false);
        this.cfg.setClassicCompatible(true);
        this.cfg.setObjectWrapper(wrapper);

        // custom directives goes here
        this.cfg.setSharedVariable("block", new BlockDirective());
        this.cfg.setSharedVariable("superBlock", new SuperBlockDirective());
        this.cfg.setSharedVariable("extends", new ExtendsDirective());
        this.cfg.setSharedVariable("transform", new TransformDirective());
        this.cfg.setSharedVariable("query", new QueryMethod());
        this.cfg.setSharedVariable("docRef", new DocRefMethod());
        this.cfg.setSharedVariable("new", new NewMethod());
        this.cfg.setSharedVariable("message", messages);
        addResourceDirectories(resourceDirs);
    }

    public void setMessageBundle(ResourceBundle messages) {
        this.messages.setBundle(messages);
    }

    public ResourceBundle getMessageBundle() {
        return messages.getBundle();
    }

    public void setResourceLocator(ResourceLocator locator) {
        this.locator = locator;
    }

    public ResourceLocator getResourceLocator() {
        return locator;
    }

    public void addResourceDirectories(File ... dirs) {
        TemplateLoader[] loaders = null;
        if (dirs != null) {
            int size = dirs.length + 2;
            loaders = new TemplateLoader[size];
            for (int i=0; i<dirs.length; i++) {
                try {
                    loaders[i] = new FileTemplateLoader(dirs[i], true);
                } catch (IOException e) {
                    e.printStackTrace(); //TODO continue?
                }
            }
            loaders[size-2] = new ResourceTemplateLoader();
            loaders[size-1] = new ClassTemplateLoader(FreemarkerEngine.class, "");
        } else {
            loaders = new TemplateLoader[2];
            loaders[0] = new ResourceTemplateLoader();
            loaders[1] = new ClassTemplateLoader(FreemarkerEngine.class, "");
        }
        this.cfg.setTemplateLoader(new MultiTemplateLoader(loaders));
    }

    public void setSharedDocumentView(RenderingContextView sharedView) {
        this.sharedView = sharedView;
    }

    public RenderingContextView getSharedDocumentView() {
        return this.sharedView;
    }

    public void setSharedVariable(String key, Object value) {
        try {
            cfg.setSharedVariable(key, value);
        }catch (Exception e) {
            e.printStackTrace();
        }
    }

    public DocumentObjectWrapper getObjectWrapper() {
        return wrapper;
    }

    public Configuration getConfiguration() {
        return cfg;
    }

    public void setTransformer(String name, RenderingTransformer transformer) {
        transformers.put(name, transformer);
    }

    public RenderingTransformer getTransformer(String name) {
        return transformers.get(name);
    }

    public void render(String template, RenderingContext ctx)
    throws RenderingException {
        render(template, ctx, null);
    }

    public void render(String template, RenderingContext ctx, Map<String,Object> globals)
    throws RenderingException {
        try {
            ObjectWrapper wrapper = getObjectWrapper();
            Template temp = cfg.getTemplate(template);
            RenderingContextModel root = new RenderingContextModel(this, ctx);
            BlockWriter bw = new BlockWriter(temp.getName(), "", new BlockWriterRegistry());
            Environment env = temp.createProcessingEnvironment(root,
                    bw, wrapper);
            if (globals != null) {
                for (Map.Entry<String, Object> entry : globals.entrySet()) {
                    env.setGlobalVariable(entry.getKey(), wrapper.wrap(entry.getValue()));
                }
            }
            env.setCustomAttribute(ROOT_CTX_KEY, root);
            env.process();
            bw.copyTo(ctx.getWriter());
        } catch (Exception e) {
            throw new RenderingException(e);
        }
    }

    public final static RenderingContextModel getContextModel() {
        return getContextModel(Environment.getCurrentEnvironment());
    }

    public final static RenderingContextModel getContextModel(Environment env) {
        return (RenderingContextModel)env.getCustomAttribute(ROOT_CTX_KEY);
    }

    class ResourceTemplateLoader extends URLTemplateLoader {

        @Override
        protected URL getURL(String arg0) {
            if (locator != null) {
                return locator.getResource(arg0);
            }
            try {
                return new URL(arg0);
            } catch (MalformedURLException e) {
                return null;
            }
        }

    }

}
