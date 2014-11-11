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
import java.util.Map;

import javax.servlet.ServletContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jboss.seam.util.DTDEntityResolver;

public class StaticNavigationHandler {

    private static final Log log = LogFactory.getLog(StaticNavigationHandler.class);

    private Map<String, String> outcomeToViewId;

    private Map<String, String> viewIdToOutcome;

    public StaticNavigationHandler(ServletContext context) {
        InputStream stream = null;
        if (context != null) {
            stream = context.getResourceAsStream("/WEB-INF/faces-config.xml");
        }

        if (stream == null) {
            log.error("No faces-config.xml file found: "
                    + "cannot resolve view id from outcome");
        } else {
            log.debug("Reading faces-config.xml");
            parse(stream);
        }
    }

    public String getOutcomeFromViewId(String viewId) {
        if (viewId == null) {
            return null;
        }
        if (viewIdToOutcome != null) {
            viewId = viewId.replace(".faces", ".xhtml");
            if (viewIdToOutcome.containsKey(viewId)) {
                return viewIdToOutcome.get(viewId);
            }
        }
        return viewId;
    }

    public String getViewIdFromOutcome(String outcome) {
        if (outcome == null) {
            return null;
        }
        if (outcomeToViewId != null) {
            if (outcomeToViewId.containsKey(outcome)) {
                return outcomeToViewId.get(outcome).replace(".xhtml", ".faces");
            }
        }
        return "/" + outcome + ".faces";
    }

    @SuppressWarnings("unchecked")
    private void parse(InputStream stream) {
        outcomeToViewId = new HashMap<String, String>();
        viewIdToOutcome = new HashMap<String, String>();
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
     */
    private static Element getDocumentRoot(InputStream stream) {
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
