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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.rendering.fm;

import java.io.Writer;
import java.util.ResourceBundle;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.platform.rendering.api.RenderingEngine;
import org.nuxeo.ecm.platform.rendering.api.RenderingException;
import org.nuxeo.ecm.platform.rendering.api.ResourceLocator;
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

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
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

    protected final LocaleMessagesMethod localeMessages = new LocaleMessagesMethod(
            null);

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
     * set the resource bundle to be used with method message and lmessage. If
     * the resourcebundle is not of the type ResourceComposite, lmessage will
     * create a default ResourceComposite.
     */
    public void setMessageBundle(ResourceBundle messages) {
        this.messages.setBundle(messages);
        if (messages instanceof ResourceComposite) {
            localeMessages.setBundle((ResourceComposite) messages);
        }
    }

    public ResourceBundle getMessageBundle() {
        return messages.getBundle();
    }

    public void setResourceLocator(ResourceLocator locator) {
        loader = new ResourceTemplateLoader(locator);
        cfg.setTemplateLoader(loader);
    }

    public ResourceLocator getResourceLocator() {
        return loader.getLocator();
    }

    public ResourceTemplateLoader getLoader() {
        return loader;
    }

    public void setSharedVariable(String key, Object value) {
        try {
            cfg.setSharedVariable(key, value);
        } catch (Exception e) {
            log.error(e, e);
        }
    }

    public DocumentObjectWrapper getObjectWrapper() {
        return wrapper;
    }

    public Configuration getConfiguration() {
        return cfg;
    }

    public void render(String template, Object input, Writer writer)
            throws RenderingException {
        try {
            /*
             * A special method to get the absolute path as an URI to be used
             * with freemarker since freemarker removes the leading / from the
             * absolute path and the file cannot be resolved anymore In the case
             * of URI like path freemarker is not modifying the path <p>
             *
             * @see TemplateCache#normalizeName()
             * @see ResourceTemplateLoader#findTemplateSource()
             */
            if (template.startsWith("/")) {
                template = "fs://" + template;
            }
            Template temp = cfg.getTemplate(template);
            BlockWriter bw = new BlockWriter(temp.getName(), "",
                    new BlockWriterRegistry());
            Environment env = temp.createProcessingEnvironment(input, bw,
                    wrapper);
            env.process();
            bw.copyTo(writer);
        } catch (Exception e) {
            throw new RenderingException(e);
        }
    }

}
