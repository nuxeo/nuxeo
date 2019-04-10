/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */
package org.nuxeo.wss.fm;

import java.io.File;
import java.io.Writer;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.wss.MSWSSConsts;
import org.nuxeo.wss.WSSConfig;
import org.nuxeo.wss.fprpc.FPRPCConts;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.core.Environment;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateModelException;

/**
 * Singleton helper class used to encapsulate FreeMarker engine.
 *
 * @author Thierry Delprat
 */
public class FreeMarkerRenderer {

    protected Configuration fmConfig;

    protected static FreeMarkerRenderer instance;

    protected static ClassTemplateLoader addLoader;

    protected PluggableTemplareLoader loader;

    public static final String FM_TEMPLATE_PATH = File.separator + "templates" + File.separator;

    private static final Log log = LogFactory.getLog(FreeMarkerRenderer.class);

    public static FreeMarkerRenderer instance() {
        if (instance == null) {
            instance = new FreeMarkerRenderer();
        }
        return instance;
    }

    protected TemplateLoader getLoader() {
        if (loader == null) {
            ClassTemplateLoader defaultLoader = new ClassTemplateLoader(this.getClass(), FM_TEMPLATE_PATH);
            loader = new PluggableTemplareLoader(defaultLoader);
            if (addLoader != null) {
                loader.setAdditionnalLoader(addLoader);
            }
        }
        return loader;
    }

    public static void addLoader(Class klass) {
        addLoader(klass, null);
    }

    public static void addLoader(Class klass, String path) {
        if (path == null) {
            path = FM_TEMPLATE_PATH;
        }
        addLoader = new ClassTemplateLoader(klass, path);
        instance = null;
    }

    public FreeMarkerRenderer() {
        fmConfig = new Configuration();
        fmConfig.setWhitespaceStripping(true);
        fmConfig.setLocalizedLookup(false);
        fmConfig.setClassicCompatible(true);
        fmConfig.setNumberFormat("0.######");
        try {
            fmConfig.setSharedVariable("config", WSSConfig.instance());
            fmConfig.setSharedVariable("MSWSSConsts", new MSWSSConsts());
            fmConfig.setSharedVariable("FPRPCConts", new FPRPCConts());
        } catch (TemplateModelException e) {
            log.error("Error creating FreeMarker engine", e);
        }

        // fmConfig.setTemplateLoader(new ClassTemplateLoader(this.getClass(),"/templates/"));

        // ClassTemplateLoader defaultLoader = new ClassTemplateLoader(this.getClass(),"/templates/");
        // TemplateLoader loader = new PluggableTemplareLoader(defaultLoader);

        fmConfig.setTemplateLoader(getLoader());
    }

    public void render(String templateName, Map<String, Object> params, Writer writer) throws Exception {
        Template template = fmConfig.getTemplate(templateName);
        Environment env = template.createProcessingEnvironment(params, writer);
        env.process();
    }

}
