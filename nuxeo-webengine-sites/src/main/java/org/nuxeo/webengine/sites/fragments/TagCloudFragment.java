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
 */

package org.nuxeo.webengine.sites.fragments;

import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBSITE;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.tag.service.api.TagCloud;
import org.nuxeo.ecm.platform.tag.service.api.TagService;
import org.nuxeo.ecm.platform.tag.service.api.WeightedTag;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.fragments.AbstractFragment;
import org.nuxeo.theme.models.Model;
import org.nuxeo.theme.models.ModelException;
import org.nuxeo.webengine.sites.models.TagCloudListModel;
import org.nuxeo.webengine.sites.models.TagCloudModel;

/**
 * Action fragment for initializing the fragment related to the details about
 * the tag cloud that have been created under a webpage, in the fragment
 * initialization mechanism.
 * 
 * @author rux
 * 
 */
public class TagCloudFragment extends AbstractFragment {

    /**
     * Returns the details about the tag cloud that have been created under a
     * webpage.
     * 
     */
    @Override
    public Model getModel() throws ModelException {
        TagCloudListModel model = new TagCloudListModel();
        try {
            TagService tagService = Framework.getService(TagService.class);
            if (tagService != null && WebEngine.getActiveContext() != null) {
                WebContext ctx = WebEngine.getActiveContext();
                CoreSession session = ctx.getCoreSession();
                DocumentModel documentModel = ctx.getTargetObject().getAdapter(
                        DocumentModel.class);

                while (!documentModel.getType().equals(WEBSITE)) {
                    documentModel = session.getParentDocument(documentModel.getRef());
                }
                if (tagService != null) {
                    TagCloudModel tagCloudModel = null;
                    TagCloud tagCloud = tagService.getPopularCloud(
                            documentModel, session.getPrincipal().getName());
                    if (tagCloud != null && !tagCloud.getListTags().isEmpty()) {
                        for (WeightedTag weightedTag : tagCloud.getListTags()) {
                            tagCloudModel = new TagCloudModel(
                                    weightedTag.getTagLabel(), Boolean.TRUE,
                                    weightedTag.getWeight());
                            model.addItem(tagCloudModel);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new ModelException(e);
        }
        return model;
    }
}
