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
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.webengine.fm.extensions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.platform.rendering.fm.extensions.BlockWriter;
import org.nuxeo.ecm.webengine.WebContext;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.themes.ThemeManager;

import freemarker.core.Environment;
import freemarker.ext.beans.BeansWrapper;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * @author <a href="mailto:jmo@chalmers.se">Jean-Marc Orliaguet</a>
 * 
 */
public class ThemeDirective implements TemplateDirectiveModel {

    private final Map<URL, String> cachedThemes = new HashMap<URL, String>();

    private Map<URL, Long> lastRefreshedMap = new HashMap<URL, Long>();

    @SuppressWarnings("unchecked")
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
            TemplateDirectiveBody body) throws TemplateException, IOException {

        if (!params.isEmpty()) {
            throw new TemplateModelException(
                    "This directive doesn't allow parameters.");
        }
        if (loopVars.length != 0) {
            throw new TemplateModelException(
                    "This directive doesn't allow loop variables.");
        }
        if (body == null) {
            throw new TemplateModelException("Expecting a body");
        }

        WebContext context = (WebContext) Utils.getWrappedObject("Context", env);

        final URL themeUrl = Utils.getThemeUrlAndSetupRequest(context);

        env.setGlobalVariable("nxthemesInfo",
                BeansWrapper.getDefaultInstance().wrap(Manager.getInfoPool()));

        BlockWriter writer = (BlockWriter) env.getOut();
        writer.setSuppressOutput(true);
        body.render(writer);
        writer.setSuppressOutput(false);

        StringReader sr = new StringReader(renderTheme(themeUrl));
        BufferedReader reader = new BufferedReader(sr);
        Configuration cfg = env.getConfiguration();
        Template temp = new Template(themeUrl.toString(), reader, cfg);
        env.include(temp);
    }

    public String renderTheme(URL themeUrl) {
        if (!needsToBeRefreshed(themeUrl) && cachedThemes.containsKey(themeUrl)) {
            return cachedThemes.get(themeUrl);
        }
        String result = ThemeManager.renderElement(themeUrl);
        if (result != null) {
            cachedThemes.put(themeUrl, result);
            lastRefreshedMap.put(themeUrl, new Date().getTime());
        }
        return result;
    }

    protected boolean needsToBeRefreshed(URL themeUrl) {
        if (themeUrl.getProtocol().equals("nxtheme")) {
            Long lastRefreshed = lastRefreshedMap.get(themeUrl);
            if (lastRefreshed == null) {
                lastRefreshed = 0L;
            }
            try {
                if (themeUrl.openConnection().getLastModified() >= lastRefreshed) {
                    return true;
                }
            } catch (IOException e) {
                return false;
            }
        }
        return false;
    }

}
