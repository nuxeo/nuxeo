/*
 * (C) Copyright 2006-2019 Nuxeo (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.rendering.fm;

import java.io.IOException;
import java.io.Writer;
import java.net.SocketException;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.rendering.api.RenderingEngine;
import org.nuxeo.ecm.platform.rendering.api.RenderingException;
import org.nuxeo.ecm.platform.rendering.api.ResourceLocator;
import org.nuxeo.ecm.platform.rendering.api.View;
import org.nuxeo.ecm.platform.rendering.fm.adapters.DocumentObjectWrapper;
import org.nuxeo.ecm.platform.rendering.fm.extensions.BlockDirective;
import org.nuxeo.ecm.platform.rendering.fm.extensions.BlockWriter;
import org.nuxeo.ecm.platform.rendering.fm.extensions.BlockWriterRegistry;
import org.nuxeo.ecm.platform.rendering.fm.extensions.DocRefMethod;
import org.nuxeo.ecm.platform.rendering.fm.extensions.ExtendsDirective;
import org.nuxeo.ecm.platform.rendering.fm.extensions.FormatDate;
import org.nuxeo.ecm.platform.rendering.fm.extensions.LocaleMessagesMethod;
import org.nuxeo.ecm.platform.rendering.fm.extensions.MessagesMethod;
import org.nuxeo.ecm.platform.rendering.fm.extensions.NewMethod;
import org.nuxeo.ecm.platform.rendering.fm.extensions.SuperBlockDirective;
import org.nuxeo.ecm.platform.rendering.fm.i18n.ResourceComposite;

import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModelException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class FreemarkerEngine implements RenderingEngine {

    private static final Log log = LogFactory.getLog(FreemarkerEngine.class);

    public static final String RENDERING_ENGINE_KEY = "NX_RENDERING_ENGINE";

    protected final Configuration cfg;

    // the wrapper is not a singleton since it contains some info about the
    // engine instance
    // so we will have one wrapper per engine instance
    protected final DocumentObjectWrapper wrapper;

    protected final MessagesMethod messages = new MessagesMethod(null);

    protected final LocaleMessagesMethod localeMessages = new LocaleMessagesMethod(null);

    protected ResourceTemplateLoader loader;

    public FreemarkerEngine() {
        this(null, null);
    }

    public FreemarkerEngine(Configuration cfg, ResourceLocator locator) {
        wrapper = new DocumentObjectWrapper(this);
        this.cfg = cfg == null ? new Configuration() : cfg;
        this.cfg.setWhitespaceStripping(true);
        this.cfg.setLocalizedLookup(false);
        this.cfg.setClassicCompatible(true);
        this.cfg.setObjectWrapper(wrapper);

        // Output encoding must not be left to null to make sure that the "?url"
        // escape utility works consistently with the expected output charset.
        // We hard-code it to UTF-8 as it's already hard-coded to UTF-8 in
        // various other places where rendering is called (such as WebEngine's
        // TemplateView or automation's FreemarkerRender).
        // TODO: expose a public getEncoding method in the RenderingEngine
        // interface and reuse it in the callers instead of hard coding the
        // charset everywhere.
        this.cfg.setOutputEncoding("UTF-8");

        // custom directives goes here
        this.cfg.setSharedVariable("block", new BlockDirective());
        this.cfg.setSharedVariable("superBlock", new SuperBlockDirective());
        this.cfg.setSharedVariable("extends", new ExtendsDirective());
        this.cfg.setSharedVariable("docRef", new DocRefMethod());
        this.cfg.setSharedVariable("new", new NewMethod());
        this.cfg.setSharedVariable("message", messages);
        this.cfg.setSharedVariable("lmessage", localeMessages);
        this.cfg.setSharedVariable("formatDate", new FormatDate());

        this.cfg.setCustomAttribute(RENDERING_ENGINE_KEY, this);
        setResourceLocator(locator);
    }

    /**
     * set the resource bundle to be used with method message and lmessage. If the resourcebundle is not of the type
     * ResourceComposite, lmessage will create a default ResourceComposite.
     */
    @Override
    public void setMessageBundle(ResourceBundle messages) {
        this.messages.setBundle(messages);
        if (messages instanceof ResourceComposite) {
            localeMessages.setBundle((ResourceComposite) messages);
        }
    }

    @Override
    public ResourceBundle getMessageBundle() {
        return messages.getBundle();
    }

    @Override
    public void setResourceLocator(ResourceLocator locator) {
        loader = new ResourceTemplateLoader(locator);
        cfg.setTemplateLoader(loader);
    }

    @Override
    public ResourceLocator getResourceLocator() {
        return loader.getLocator();
    }

    public ResourceTemplateLoader getLoader() {
        return loader;
    }

    @Override
    public void setSharedVariable(String key, Object value) {
        try {
            cfg.setSharedVariable(key, value);
        } catch (TemplateModelException e) {
            log.error(e, e);
        }
    }

    public DocumentObjectWrapper getObjectWrapper() {
        return wrapper;
    }

    public Configuration getConfiguration() {
        return cfg;
    }

    @Override
    public View getView(String path) {
        return new View(this, path);
    }

    @Override
    public View getView(String path, Object object) {
        return new View(this, path, object);
    }

    @Override
    public void render(String template, Object input, Writer writer) throws RenderingException {
        try {
            /*
             * A special method to get the absolute path as an URI to be used with freemarker since freemarker removes
             * the leading / from the absolute path and the file cannot be resolved anymore In the case of URI like path
             * freemarker is not modifying the path <p>
             * @see TemplateCache#normalizeName()
             * @see ResourceTemplateLoader#findTemplateSource()
             */
            if (template.startsWith("/")) {
                template = "fs://" + template;
            }
            Template temp = cfg.getTemplate(template);
            @SuppressWarnings("resource") // BlockWriter chaining makes close() hazardous
            BlockWriter bw = new BlockWriter(temp.getName(), "", new BlockWriterRegistry());
            Environment env = temp.createProcessingEnvironment(input, bw, wrapper);
            env.process();
            bw.copyTo(writer);
        } catch (SocketException e) {
            log.debug("Output closed while rendering " + template);
        } catch (IOException | TemplateException e) {
            throw new RenderingException(e);
        }
    }

    @Override
    public void flushCache() {
        cfg.clearTemplateCache();
    }

}
