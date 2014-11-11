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

import org.jboss.seam.core.Events;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.annotations.Operation;
import org.nuxeo.ecm.automation.core.annotations.OperationMethod;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.util.StringList;
import org.nuxeo.ecm.automation.jsf.OperationHelper;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.api.NavigationContext;
import org.nuxeo.ecm.webapp.helpers.EventNames;

/**
 * Operation that:
 * <ul>
 * <li>refreshes the content view cache;</li>
 * <li>raises standard seam events to trigger refresh of most seam component;</li>
 * <li>raises additional configurable seam events.</li>
 * </ul>
 *
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 */
@Operation(id = RefreshUI.ID, category = Constants.CAT_UI, requires = Constants.SEAM_CONTEXT, label = "Refresh", description = "Refresh the UI cache. This is a void operation - the input object is returned back as the oputput")
public class RefreshUI {

    public static final String ID = "Seam.Refresh";

    /**
     * Additional list of seam event names to raise
     *
     * @since 5.5
     */
    @Param(name = "additional list of seam events to raise", required = false)
    protected StringList additionalSeamEvents;

    @OperationMethod
    public void run() {
        OperationHelper.getContentViewActions().resetAllContent();
        NavigationContext context = OperationHelper.getNavigationContext();
        DocumentModel dm = context.getCurrentDocument();
        if (dm != null) {
            Events.instance().raiseEvent(EventNames.DOCUMENT_CHANGED, dm);
            Events.instance().raiseEvent(EventNames.DOCUMENT_CHILDREN_CHANGED,
                    dm);
        }
        if (additionalSeamEvents != null) {
            for (String event : additionalSeamEvents) {
                Events.instance().raiseEvent(event);
            }
        }
    }

}
