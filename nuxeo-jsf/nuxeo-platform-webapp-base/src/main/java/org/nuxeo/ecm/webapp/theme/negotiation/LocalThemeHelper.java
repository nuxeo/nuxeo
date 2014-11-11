/*
 * (C) Copyright 2006 Nuxeo SAS <http://nuxeo.com> and others
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

package org.nuxeo.ecm.webapp.theme.negotiation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ec.placeful.Annotation;
import org.nuxeo.ecm.platform.ec.placeful.interfaces.PlacefulService;
import org.nuxeo.runtime.api.Framework;

public class LocalThemeHelper {

    private static final Log log = LogFactory.getLog(LocalThemeHelper.class);

    public static PlacefulService getPlacefulServiceBean() {
        PlacefulService placefulService = null;
        try {
//            placefulService = ECM.getPlatform().getService(
//                    EJBPlacefulService.class);
            placefulService= Framework.getService(PlacefulService.class);
        } catch (Exception e) {
            log.error("Error connecting to PlacefulService", e);
        }
        if (null == placefulService) {
            log.error("Placeful service not bound");
        }
        return placefulService;
    }

    public static LocalThemeConfig getLocalThemeConfig(DocumentModel doc) {
        PlacefulService placefulService = getPlacefulServiceBean(); //LocalThemeHelper.getPlacefulServiceBean()
        if (placefulService == null) {
            return null;
        }
        String docId = doc.getId();
        if (docId == null) {
            log.error("Could not get the current document's uuid.");
            return null;
        }
        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("docId", docId);
        List<Annotation> configs;
        configs = placefulService.getAnnotationListByParamMap(paramMap,
                LocalThemeConfig.LOCAL_THEME_NAME);
        if (!configs.isEmpty()) {
            return (LocalThemeConfig) configs.get(0);
        }
        return null;
    }

    public static void setLocalThemeConfig(String theme, String page,
            String perspective, String engine, String mode, DocumentModel doc) {
        PlacefulService placefulService = getPlacefulServiceBean();
        if (placefulService == null) {
            return;
        }
        String docId = doc.getId();
        if (docId == null) {
            log.error("Could not get the current document's uuid.");
            return;
        }
        // Remove the old configuration
        removeLocalThemeConfig(doc);

        // Set the new values
        if ("".equals(theme)) {
            theme = null;
        }
        if (theme == null || "".equals(page)) {
            page = null;
        }
        if ("".equals(perspective)) {
            perspective = null;
        }
        if ("".equals(engine)) {
            engine = null;
        }
        if ("".equals(mode)) {
            mode = null;
        }
        if (theme != null || page != null || perspective != null
                || engine != null || mode != null) {
            placefulService.setAnnotation(new LocalThemeConfig(theme, page,
                    perspective, engine, mode, docId));
        }
    }

    public static void removeLocalThemeConfig(DocumentModel doc) {
        PlacefulService placefulService = getPlacefulServiceBean();
        if (placefulService == null) {
            return;
        }
        String docId = doc.getId();
        if (docId == null) {
            log.error("Could not get the current document's uuid.");
            return;
        }
        Map<String, Object> paramMap = new HashMap<String, Object>();
        paramMap.put("docId", docId);
        placefulService.removeAnnotationListByParamMap(paramMap,
                    LocalThemeConfig.LOCAL_THEME_NAME);
    }

}
