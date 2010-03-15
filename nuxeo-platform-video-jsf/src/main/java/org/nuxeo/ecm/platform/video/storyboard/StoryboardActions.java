/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.video.storyboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.video.VideoConstants;

/**
 * Backing bean for the Storyboard view of an document with the video storyboard
 * facet.
 *
 * @author ogrisel
 */
@Name("storyboardActions")
@Scope(ScopeType.EVENT)
public class StoryboardActions {

    public List<StoryboardItem> getItems(DocumentModel doc)
            throws PropertyException, ClientException {
        if (!doc.hasFacet(VideoConstants.HAS_STORYBOARD_FACET)) {
            return Collections.emptyList();
        }
        int size = doc.getProperty(VideoConstants.STORYBOARD_PROPERTY).getValue(
                List.class).size();
        List<StoryboardItem> items = new ArrayList<StoryboardItem>(size);
        for (int i = 0; i < size; i++) {
            items.add(new StoryboardItem(doc,
                    VideoConstants.STORYBOARD_PROPERTY, i));
        }
        return items;
    }

}
