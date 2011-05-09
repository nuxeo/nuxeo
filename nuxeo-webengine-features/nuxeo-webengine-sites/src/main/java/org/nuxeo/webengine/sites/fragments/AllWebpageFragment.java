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

import org.nuxeo.common.utils.URIUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.theme.fragments.AbstractFragment;
import org.nuxeo.theme.models.Model;
import org.nuxeo.theme.models.ModelException;
import org.nuxeo.webengine.sites.models.WebpageListModel;
import org.nuxeo.webengine.sites.models.WebpageModel;
import org.nuxeo.webengine.sites.utils.SiteConstants;
import org.nuxeo.webengine.sites.utils.SiteUtils;

/**
 * Action fragment for initializing the fragment related to the <b>WebPage</b>-s
 * that are direct children of the received document.
 *
 * @author rux
 */
public class AllWebpageFragment extends AbstractFragment {

    /**
     * Returns all the <b>WebPage</b>-s that are direct children of the received
     * document.
     */
    @Override
    public Model getModel() throws ModelException {
        WebpageListModel model = new WebpageListModel();
        if (WebEngine.getActiveContext() != null) {
            WebContext ctx = WebEngine.getActiveContext();
            CoreSession session = ctx.getCoreSession();
            DocumentModel documentModel = ctx.getTargetObject().getAdapter(
                    DocumentModel.class);

            try {
                for (DocumentModel webPage : session.getChildren(
                        documentModel.getRef(), SiteConstants.WEBPAGE)) {
                    if (!webPage.getCurrentLifeCycleState().equals(
                            SiteConstants.DELETED)) {
                        String name = SiteUtils.getString(webPage, "dc:title");
                        String path = URIUtils.quoteURIPathComponent(webPage.getName(), false);
                        WebpageModel webpageModel = new WebpageModel(name, path);
                        model.addItem(webpageModel);
                    }
                }
            } catch (Exception e) {
                throw new ModelException(e);
            }

        }
        return model;
    }

}
