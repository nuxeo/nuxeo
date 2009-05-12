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

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.tag.service.api.TagService;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.fragments.AbstractFragment;
import org.nuxeo.theme.models.Model;
import org.nuxeo.theme.models.ModelException;
import org.nuxeo.webengine.sites.models.TagListModel;
import org.nuxeo.webengine.sites.models.TagModel;
import org.nuxeo.webengine.sites.utils.SiteConstants;
import org.nuxeo.webengine.sites.utils.SiteUtils;

/**
 * Action fragment for initializing the fragment related to the list with the
 * details about the <b>Tag</b>-s that have been created under a webpage, in the
 * fragment initialization mechanism.
 * 
 * @author rux
 * 
 */
public class TagFragment extends AbstractFragment {

    /**
     * Returns the list with the details about the <b>Tag</b>-s that have been
     * created under a webpage.
     * 
     */
    @Override
    public Model getModel() throws ModelException {
        TagListModel model = new TagListModel();
        try {
            TagService tagService = Framework.getService(TagService.class);
            if (tagService != null && WebEngine.getActiveContext() != null) {
                WebContext ctx = WebEngine.getActiveContext();
                CoreSession session = ctx.getCoreSession();
                if (tagService != null) {

                    TagModel tagModel = null;
                    String label = null;
                    Boolean isPrivate = null;
                    DocumentModelList tags = tagService.listTagsInGroup(
                            tagService.getRootTag("default"),
                            session.getPrincipal().getName());
                    if (tags != null && !tags.isEmpty()) {
                        for (DocumentModel document : tags) {
                            if (!document.getCurrentLifeCycleState().equals(
                                    SiteConstants.DELETED)) {

                                label = SiteUtils.getString(document,
                                        "tag:label");
                                isPrivate = SiteUtils.getBoolean(document,
                                        "tag:private");
                                tagModel = new TagModel(label, isPrivate);
                                model.addItem(tagModel);

                            }
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
