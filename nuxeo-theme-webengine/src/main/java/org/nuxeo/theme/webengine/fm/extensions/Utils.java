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
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.webengine.WebContext;
import org.nuxeo.theme.ApplicationType;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.NegotiationDef;
import org.nuxeo.theme.negotiation.NegotiationException;
import org.nuxeo.theme.types.TypeFamily;
import org.nuxeo.theme.webengine.negotiation.WebNegotiator;

import freemarker.core.Environment;
import freemarker.ext.beans.BeanModel;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * @author <a href="mailto:jmo@chalmers.se">Jean-Marc Orliaguet</a>
 * 
 */
public class Utils {

    private static final Log log = LogFactory.getLog(Utils.class);

    public static Object getWrappedObject(String name, Environment env) {
        Object obj = null;
        try {
            obj = env.getVariable(name);
            if (obj != null) {
                if (obj == TemplateModel.NOTHING) {
                    obj = null;
                } else if (obj instanceof BeanModel) {
                    BeanModel bean = (BeanModel) obj;
                    obj = bean.getWrappedObject();
                } else if (obj instanceof SimpleScalar) {
                    obj = obj.toString();
                }
            }
        } catch (TemplateModelException e) {
            log.info(e);
        }
        return obj;
    }

    public static URL getThemeUrlAndSetupRequest(WebContext context)
            throws IOException {
        HttpServletRequest request = context.getRequest();
        URL themeUrl = (URL) request.getAttribute("org.nuxeo.theme.url");
        if (themeUrl != null) {
            return themeUrl;
        }
        
        HttpServletResponse response = context.getResponse();

        // Get the negotiation strategy
        final String root = context.getApplicationPath();

        final ApplicationType application = (ApplicationType) Manager.getTypeRegistry().lookup(
                TypeFamily.APPLICATION, root);

        String strategy = null;
        if (application != null) {
            final NegotiationDef negotiation = application.getNegotiation();
            if (negotiation != null) {
                request.setAttribute("org.nuxeo.theme.default.theme",
                        negotiation.getDefaultTheme());
                request.setAttribute("org.nuxeo.theme.default.engine",
                        negotiation.getDefaultEngine());
                request.setAttribute("org.nuxeo.theme.default.perspective",
                        negotiation.getDefaultPerspective());
                strategy = negotiation.getStrategy();
            }
        }

        if (strategy == null) {
            log.error("Could not obtain the negotiation strategy for " + root);
            // FIXME
            response.sendRedirect("/nuxeo/nxthemes/error/negotiationStrategyNotSet.faces");
        } else {
            try {
                final String spec = new WebNegotiator(strategy, context).getSpec();
                themeUrl = new URL(spec);
                request.setAttribute("org.nuxeo.theme.url", themeUrl);
            } catch (NegotiationException e) {
                log.error("Could not get default negotiation settings.", e);
                // FIXME
                response.sendRedirect("/nuxeo/nxthemes/error/negotiationDefaultValuesNotSet.faces");
            }
        }

        return themeUrl;
    }
    
    public static Map<String, String> getTemplateDirectiveParameters(Map<String, TemplateModel> params) {
        Map<String, String> attributes = new HashMap<String, String>();
        for (Map.Entry<String,TemplateModel> entry : params.entrySet()) {
            TemplateModel v = entry.getValue();
            attributes.put(entry.getKey(), v.toString());
        }
        return attributes;
    }
}
