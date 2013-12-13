/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 */

package org.nuxeo.ecm.automation.jsf.operations;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.core.Events;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.automation.jsf.OperationHelper;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 * Operation that:
 * <ul>
 * <li>refreshes the content view cache;</li>
 * <li>refreshes the navigation context current document cache;</li>
 * <li>raises standard seam events to trigger refresh of most seam component;</li>
 * <li>raises additional configurable seam events.</li>
 * </ul>
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@Operation(id = RefreshUI.ID, category = Constants.CAT_UI, requires = Constants.SEAM_CONTEXT, label = "Refresh", description = "Refresh the UI cache. This is a void operation - the input object is returned back as the oputput")
public class RefreshUI {

    public static final String ID = "Seam.Refresh";

    protected static final Log log = LogFactory.getLog(RefreshUI.class);

    /**
     * Additional list of seam event names to raise
     *
     * @since 5.5
     */
    @Param(name = "additional list of seam events to raise", required = false)
    protected StringList additionalSeamEvents;

    @OperationMethod
    public void run() {
        if (OperationHelper.isSeamContextAvailable()) {
            OperationHelper.getContentViewActions().resetAllContent();
            NavigationContext navContext = OperationHelper.getNavigationContext();
            try {
                navContext.invalidateCurrentDocument();
            } catch (ClientException e) {
                log.error("Error invalidating current document "
                        + "on navigation context", e);
            }
            DocumentModel dm = navContext.getCurrentDocument();
            if (dm != null) {
                Events.instance().raiseEvent(EventNames.DOCUMENT_CHANGED, dm);
                Events.instance().raiseEvent(
                        EventNames.DOCUMENT_CHILDREN_CHANGED, dm);
            }
            if (additionalSeamEvents != null) {
                for (String event : additionalSeamEvents) {
                    Events.instance().raiseEvent(event);
                }
            }
        } else {
            log.debug("Skip Seam.Refresh operation since Seam context has not been initialized");
        }
    }

}
