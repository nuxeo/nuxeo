/*
 * (C) Copyright 2009-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Radu Darlea
 *     Florent Guillaume
 */
package org.nuxeo.webengine.sites.fragments;

import java.util.List;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.tag.Tag;
import org.nuxeo.ecm.platform.tag.TagService;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.fragments.AbstractFragment;
import org.nuxeo.theme.models.Model;
import org.nuxeo.theme.models.ModelException;
import org.nuxeo.webengine.sites.models.TagListModel;
import org.nuxeo.webengine.sites.models.TagModel;

/**
 * Action fragment for initializing the fragment related to the list with the
 * details about the <b>Tag</b>-s that have been created under a web page, in
 * the fragment initialization mechanism.
 */
public class TagFragment extends AbstractFragment {

    /**
     * Returns the list with the details about the <b>Tag</b>-s that have been
     * created under a web page.
     */
    @Override
    public Model getModel() throws ModelException {
        TagListModel model = new TagListModel();
        try {
            TagService tagService = Framework.getService(TagService.class);
            if (tagService == null || WebEngine.getActiveContext() == null
                    || !tagService.isEnabled()) {
                return model;
            }
            WebContext ctx = WebEngine.getActiveContext();
            CoreSession session = ctx.getCoreSession();
            DocumentModel documentModel = ctx.getTargetObject().getAdapter(
                    DocumentModel.class);
            List<Tag> tags = tagService.getDocumentTags(session,
                    documentModel.getId(), null);
            if (tags == null || tags.isEmpty()) {
                return model;
            }

            for (Tag tag : tags) {
                TagModel tagModel = new TagModel(tag.label, true);
                model.addItem(tagModel);
            }
            return model;
        } catch (Exception e) {
            throw new ModelException(e);
        }
    }

}
