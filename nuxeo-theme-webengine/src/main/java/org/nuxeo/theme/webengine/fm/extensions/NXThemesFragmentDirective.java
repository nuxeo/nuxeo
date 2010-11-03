/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.theme.models.InfoPool;
import org.nuxeo.theme.themes.ThemeException;
import org.nuxeo.theme.themes.ThemeManager;

import freemarker.core.Environment;
import freemarker.ext.beans.BeansWrapper;
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
public class NXThemesFragmentDirective implements TemplateDirectiveModel {

    private static final Log log = LogFactory.getLog(NXThemesFragmentDirective.class);

    final String templateEngine = "freemarker";

    @SuppressWarnings("unchecked")
    public void execute(Environment env, Map params, TemplateModel[] loopVars,
            TemplateDirectiveBody body) throws TemplateException, IOException {

        if (loopVars.length != 0) {
            throw new TemplateModelException(
                    "This directive doesn't allow loop variables.");
        }
        if (body != null) {
            throw new TemplateModelException("Didn't expect a body");
        }

        WebContext ctx = WebEngine.getActiveContext();
        if (ctx == null) {
            throw new IllegalStateException("Not In a Web Context");
        }

        env.setVariable("nxthemesInfo",
                BeansWrapper.getDefaultInstance().wrap(InfoPool.getInfoMap()));

        Map<String, String> attributes = Utils.getTemplateDirectiveParameters(params);
        final URL elementUrl = new URL(String.format(
                "nxtheme://element/%s/%s/%s/%s", attributes.get("engine"),
                attributes.get("mode"), templateEngine, attributes.get("uid")));

        String rendered = "";
        try {
            rendered = ThemeManager.renderElement(elementUrl);
        } catch (ThemeException e) {
            log.error("Element rendering failed: " + e.getMessage());
            return;
        }
        StringReader sr = new StringReader(rendered);
        BufferedReader reader = new BufferedReader(sr);
        Template tpl = new Template(elementUrl.toString(), reader,
                env.getConfiguration(), env.getTemplate().getEncoding());

        try {
            env.include(tpl);
        } catch (Exception e) {
            log.error("Rendering of Freemarker template failed: \n" + rendered, e);
        } finally {
            reader.close();
            sr.close();
        }

    }

}
