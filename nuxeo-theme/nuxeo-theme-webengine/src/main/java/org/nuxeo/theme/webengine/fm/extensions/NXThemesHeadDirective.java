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

import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.webengine.WebContext;
import org.nuxeo.theme.html.ui.Head;
import org.nuxeo.theme.themes.ThemeManager;

import freemarker.core.Environment;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * @author <a href="mailto:jmo@chalmers.se">Jean-Marc Orliaguet</a>
 * 
 */
public class NXThemesHeadDirective implements TemplateDirectiveModel {

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
        if (body != null) {
            throw new TemplateModelException("Didn't expect a body");
        }

        Writer writer = env.getOut();
        
        WebContext context = (WebContext) Utils.getWrappedObject("Context", env);
        final URL themeUrl = Utils.getThemeUrlAndSetupRequest(context);

        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("themeName", ThemeManager.getThemeNameByUrl(themeUrl));
        attributes.put("basePath", context.getBasePath());
        attributes.put(
                "baseUrl",
                context.getBasePath());
        writer.write(Head.render(attributes));
        
        
    }
}
