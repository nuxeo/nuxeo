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
import org.nuxeo.theme.negotiation.Scheme;
import org.nuxeo.theme.perspectives.PerspectiveType;

/**
 * Negotiation scheme for obtaining the local perspective from the current
 * document.
 *
 * @author <a href="mailto:jmo@chalmers.se">Jean-Marc Orliaguet</a>
 */
public class LocalPerspective implements Scheme {

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

        // Extract the perspective
        String perspectiveName = localThemeConfig.getPerspective();
        if (perspectiveName == null) {
            return null;
        }

        // Look up the perspective
        PerspectiveType perspective = Manager.getPerspectiveManager().getPerspectiveByName(
                perspectiveName);
        if (perspective != null) {
            return perspectiveName;
        }
        return null;
    }

}
