/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.html.ui;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.web.common.vh.VirtualHostHelper;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.resources.ResourceManager;

public class Resources {

    public static String render(Map<String, String> params,
            boolean virtualHosting) {
        StringBuilder sb = new StringBuilder();

        String resourcePath = VirtualHostHelper.getContextPathProperty()
                + "/nxthemes-lib/";
        final String themeUrl = params.get("themeUrl");
        final String path = params.get("path");
        final String basepath = params.get("basepath");
        String nxthemeBasePath = basepath;

        final ResourceManager resourceManager = Manager.getResourceManager();

        final StringBuilder combinedStyles = new StringBuilder();
        final StringBuilder combinedScripts = new StringBuilder();
        combinedStyles.append(resourcePath);
        combinedScripts.append(resourcePath);

        boolean hasScripts = false;
        boolean hasStyles = false;

        boolean ignoreLocal = false;
        if (params.containsKey("ignoreLocal")) {
            ignoreLocal = Boolean.parseBoolean(params.get("ignoreLocal"));
        }

        List<String> resourceNames;
        if (ignoreLocal) {
            resourceNames = resourceManager.getGlobalResourcesFor(themeUrl);
        } else {
            resourceNames = resourceManager.getResourcesFor(themeUrl);
        }

        for (String resourceName : resourceNames) {
            if (resourceName.endsWith(".css")) {
                combinedStyles.append(resourceName).append(",");
                hasStyles = true;
            } else if (resourceName.endsWith(".js")) {
                combinedScripts.append(resourceName).append(",");
                hasScripts = true;
            }
        }

        combinedStyles.deleteCharAt(combinedStyles.length() - 1);
        combinedScripts.deleteCharAt(combinedScripts.length() - 1);
        combinedStyles.append("?path=").append(path).append("&amp;basepath=").append(
                basepath);
        combinedScripts.append("?path=").append(path).append("&amp;basepath=").append(
                basepath);

        // styles
        if (hasStyles) {
            sb.append(String.format(
                    "<link type=\"text/css\" rel=\"stylesheet\" media=\"all\" href=\"%s\"/>",
                    combinedStyles.toString()));
        }

        final String contextPath = params.get("contextPath");
        // scripts
        sb.append(String.format("<script type=\"text/javascript\"><!--\n"
                + "var nxthemesPath = \"%s\";\n"
                + "var nxthemesBasePath = \"%s\";\n"
                + "var nxContextPath = \"%s\";\n" + "//--></script>\n", path,
                nxthemeBasePath, contextPath));
        if (hasScripts) {
            sb.append(String.format(
                    "<script type=\"text/javascript\" src=\"%s\"></script>",
                    combinedScripts.toString()));
        }

        // Flush local resources
        resourceManager.flush();
        return sb.toString();
    }

}
