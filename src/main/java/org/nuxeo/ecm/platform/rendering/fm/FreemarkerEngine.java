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

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.platform.rendering.api.EnvironmentProvider;
import org.nuxeo.ecm.platform.rendering.api.RenderingContext;
import org.nuxeo.ecm.platform.rendering.api.RenderingEngine;
import org.nuxeo.ecm.platform.rendering.api.RenderingException;
import org.nuxeo.ecm.platform.rendering.api.RenderingTransformer;
import org.nuxeo.ecm.platform.rendering.api.ResourceLocator;
import org.nuxeo.ecm.platform.rendering.fm.adapters.DocumentObjectWrapper;
import org.nuxeo.ecm.platform.rendering.fm.adapters.RootContextModel;
import org.nuxeo.ecm.platform.rendering.fm.extensions.RenderDirective;
import org.nuxeo.ecm.platform.rendering.fm.extensions.TransformDirective;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.cache.URLTemplateLoader;
import freemarker.core.Environment;
import freemarker.template.Configuration;
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
    protected EnvironmentProvider envProvider = new EnvironmentProvider() {
       public Object getEnv(String key, RenderingContext ctx) {
            if ("engine".equals(key)) {
                return "Nuxeo Freemarker Engine";
            } else if ("version".equals(key)) {
                return "1.0.0";
            }
            return null;
        }
       public Collection<String> getKeys() {
           return Arrays.asList("engine", "version");
       }
       public int size() {
        return 2;
       }
    };

    //TODO add possibility to register new views by doc types through extension points
    protected DocumentView view = DocumentView.DEFAULT;

    protected Map<String, RenderingTransformer> transformers = new HashMap<String, RenderingTransformer>();


    public FreemarkerEngine() {
        this (null, new Configuration());
    }

    /**
     *
     */
    public FreemarkerEngine(ResourceLocator locator, Configuration cfg) {
        this.wrapper = new DocumentObjectWrapper(this);
        this.locator = locator;
        this.cfg = cfg;
        this.cfg.setTemplateLoader(new MultiTemplateLoader(
                new TemplateLoader[] {
                        new ResourceTemplateLoader(),
                        new ClassTemplateLoader(FreemarkerEngine.class, "")
                         }));
        this.cfg.setWhitespaceStripping(true);
        this.cfg.setLocalizedLookup(false);
        this.cfg.setClassicCompatible(true);
        this.cfg.setObjectWrapper(wrapper);

        // custom directives goes here
        this.cfg.setSharedVariable("render", new RenderDirective());
        this.cfg.setSharedVariable("transform", new TransformDirective());
    }

    public void setResourceLocator(ResourceLocator locator) {
        this.locator = locator;
    }

    public ResourceLocator getResourceLocator() {
        return locator;
    }

    public void setEnvironmentProvider(EnvironmentProvider env) {
        envProvider = env;
    }

    public EnvironmentProvider getEnvironmentProvider() {
        return envProvider;
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

    public DocumentView getDocumentView() {
        return view;
    }

    public void setDocumentView(DocumentView view) {
        if (view == null) view = DocumentView.DEFAULT;
        this.view = view;
    }

    public void setTransformer(String name, RenderingTransformer transformer) {
        transformers.put(name, transformer);
    }

    public RenderingTransformer getTransformer(String name) {
        return transformers.get(name);
    }

    public void render(RenderingContext ctx)
    throws RenderingException {
        try {
            Template temp = cfg.getTemplate(ctx.getTemplate());
            RootContextModel root = new RootContextModel(this, ctx);
            Environment env = temp.createProcessingEnvironment(root,
                    ctx.getWriter(), getObjectWrapper());
            env.setCustomAttribute(ROOT_CTX_KEY, root);
            env.process();
        } catch (Exception e) {
            throw new RenderingException(e);
        }
    }

    public final static RootContextModel getRootContext() {
        return getRootContext(Environment.getCurrentEnvironment());
    }

    public final static RootContextModel getRootContext(Environment env) {
        return (RootContextModel)env.getCustomAttribute(ROOT_CTX_KEY);
    }

    class ResourceTemplateLoader extends URLTemplateLoader {

        @Override
        protected URL getURL(String arg0) {
            if (locator != null) {
                return locator.getResource(arg0);
            }
            return null;
        }

    }

}
