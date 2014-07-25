/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: StaticNavigationHandler.java 21462 2007-06-26 21:16:36Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.rest;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.faces.application.NavigationCase;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jboss.seam.util.DTDEntityResolver;
import org.nuxeo.runtime.api.Framework;

import com.sun.faces.application.ApplicationAssociate;

/**
 * View id helper that matches view ids and outcomes thanks to navigation cases
 * defined in a faces-config.xml file.
 * <p>
 * Also handle some hot reload cases, by parsing the main faces-config.xml
 * file.
 */
public class StaticNavigationHandler {

    private static final Log log = LogFactory.getLog(StaticNavigationHandler.class);

    private final HashMap<String, String> outcomeToViewId = new HashMap<String, String>();

    private final HashMap<String, String> viewIdToOutcome = new HashMap<String, String>();

    public StaticNavigationHandler(ServletContext context) {
        ApplicationAssociate associate = ApplicationAssociate.getCurrentInstance();
        FacesContext faces = FacesContext.getCurrentInstance();
        for (Set<NavigationCase> cases : associate.getNavigationCaseListMappings().values()) {
            for (NavigationCase cnc : cases) {
                // FIXME: cases view ids now require the faces context not to be null
                // String toViewId = cnc.getToViewId(faces);
                // String fromOutcome = cnc.getFromOutcome();
                // outcomeToViewId.put(fromOutcome, toViewId);
                // viewIdToOutcome.put(toViewId, fromOutcome);
            }
        }
        // FIXME: disable this when not in dev mode, until above nav is fixed
        if (Framework.isDevModeSet() || true) {
            handleHotReloadResources(context);
        }
    }

    public String getOutcomeFromViewId(String viewId) {
        if (viewId == null) {
            return null;
        }
        viewId = viewId.replace(".faces", ".xhtml");
        if (viewIdToOutcome.containsKey(viewId)) {
            return viewIdToOutcome.get(viewId);
        }
        return viewId;
    }

    public String getViewIdFromOutcome(String outcome) {
        if (outcome == null) {
            return null;
        }
        if (outcomeToViewId.containsKey(outcome)) {
            return outcomeToViewId.get(outcome).replace(".xhtml", ".faces");
        }
        // try to guess the view name
        String viewId = "/" + outcome + ".faces";
        log.warn(String.format("Guessing view id for outcome '%s': use '%s'",
                outcome, viewId));
        return viewId;
    }

    /**
     * XXX hack: add manual parsing of the main faces-config.xml file
     * navigation cases, to handle hot reload and work around the JSF
     * application cache.
     * <p>
     * TODO: try to reset and rebuild the app navigation cases by reflection,
     * if it works...
     *
     * @since 5.6
     */
    protected void handleHotReloadResources(ServletContext context) {
        InputStream stream = null;
        if (context != null) {
            stream = context.getResourceAsStream("/WEB-INF/faces-config.xml");
        }
        if (stream != null) {
            parse(stream);
        }
    }

    /**
     * @since 5.6
     */
    @SuppressWarnings("unchecked")
    protected void parse(InputStream stream) {
        Element root = getDocumentRoot(stream);
        List<Element> elements = root.elements("navigation-rule");
        for (Element rule : elements) {
            List<Element> nav_cases = rule.elements("navigation-case");
            for (Element nav_case : nav_cases) {
                Element from_el = nav_case.element("from-outcome");
                Element to_el = nav_case.element("to-view-id");

                if ((from_el != null) && (to_el != null)) {
                    String from = from_el.getTextTrim();
                    String to = to_el.getTextTrim();
                    outcomeToViewId.put(from, to);
                    viewIdToOutcome.put(to, from);
                }
            }
        }
    }

    /**
     * Gets the root element of the document.
     *
     * @since 5.6
     */
    protected static Element getDocumentRoot(InputStream stream) {
        try {
            SAXReader saxReader = new SAXReader();
            saxReader.setEntityResolver(new DTDEntityResolver());
            saxReader.setMergeAdjacentText(true);
            return saxReader.read(stream).getRootElement();
        } catch (DocumentException de) {
            throw new RuntimeException(de);
        }
    }

}
