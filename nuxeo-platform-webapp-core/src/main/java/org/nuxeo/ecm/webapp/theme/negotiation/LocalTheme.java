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

package org.nuxeo.ecm.webapp.theme.negotiation;

import org.jboss.seam.Component;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.elements.PageElement;
import org.nuxeo.theme.negotiation.Scheme;

/**
 * Negotiation scheme for obtaining the local theme from the current space.
 *
 * @author <a href="mailto:jmo@chalmers.se">Jean-Marc Orliaguet</a>
 */
public class LocalTheme implements Scheme {

    /**
     * Called by the theme negotiation module.
     *
     * @return the local theme associated to the current space (workspace,
     *         section, ...) as a 'theme/page' string. Return null otherwise.
     */
    public String getOutcome(Object context) {
        DocumentModel currentSuperSpace = (DocumentModel) Component.getInstance("currentSuperSpace");
        if (currentSuperSpace == null) {
            return null;
        }

        // Get the placeful local theme configuration for the current workspace.
        LocalThemeConfig localThemeConfig = LocalThemeHelper.getLocalThemeConfig(currentSuperSpace);
        if (localThemeConfig == null) {
            return null;
        }

        // Extract the page path
        String path = localThemeConfig.computePagePath();
        if (path == null) {
            return null;
        }

        // Look up the page
        PageElement page = Manager.getThemeManager().getPageByPath(path);
        if (page != null) {
            return path;
        }
        return null;
    }

}
