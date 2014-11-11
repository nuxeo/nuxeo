/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     rdarlea
 */
package org.nuxeo.webengine.sites.fragments;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.theme.fragments.AbstractFragment;
import org.nuxeo.theme.models.Model;
import org.nuxeo.theme.models.ModelException;
import org.nuxeo.webengine.sites.models.ContextualLinkListModel;
import org.nuxeo.webengine.sites.models.ContextualLinkModel;
import org.nuxeo.webengine.sites.utils.SiteConstants;
import org.nuxeo.webengine.sites.utils.SiteUtils;

/**
 * Action fragment for initializing the fragment related to the list with the
 * details about the <b>Contextual Link</b>-s that have been created under a
 * <b>WebSite</b> or <b>Webpage</b> document type.
 *
 * @author rux
 */
public class ContextualLinkFragment extends AbstractFragment {

    /**
     * Returns the list with the details about the <b>Contextual Link</b>-s that
     * have been created under a <b>WebSite</b> or <b>Webpage</b> document
     * type.
     */
    @Override
    public Model getModel() throws ModelException {
        ContextualLinkListModel model = new ContextualLinkListModel();
        if (WebEngine.getActiveContext() != null) {
            WebContext ctx = WebEngine.getActiveContext();
            CoreSession session = ctx.getCoreSession();
            DocumentModel documentModel = ctx.getTargetObject().getAdapter(
                    DocumentModel.class);

            if (documentModel.hasFacet(SiteConstants.WEB_VIEW_FACET)
                    || SiteConstants.WEBPAGE.equals(documentModel.getType())) {
                // Is WebSite (has 'WebView' facet) or WebPage.

                try {
                    for (DocumentModel document : session.getChildren(
                            documentModel.getRef(),
                            SiteConstants.CONTEXTUAL_LINK)) {
                        if (!document.getCurrentLifeCycleState().equals(
                                SiteConstants.DELETED)) {

                            String title = SiteUtils.getString(document, "dc:title");
                            String description = SiteUtils.getString(document,
                                    "dc:description");
                            String link = SiteUtils.getString(document, "clink:link");
                            ContextualLinkModel linkModel = new ContextualLinkModel(title,
                                    description, link);
                            model.addItem(linkModel);
                        }
                    }
                } catch (Exception e) {
                    throw new ModelException(e);
                }

            }
        }
        return model;
    }

}
