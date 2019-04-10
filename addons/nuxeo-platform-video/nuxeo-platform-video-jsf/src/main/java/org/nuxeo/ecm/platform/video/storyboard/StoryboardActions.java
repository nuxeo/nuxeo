/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.video.storyboard;

import static org.nuxeo.ecm.platform.video.VideoConstants.STORYBOARD_PROPERTY;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.platform.video.VideoConstants;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Backing bean for the Storyboard view of an document with the video storyboard facet.
 *
 * @author ogrisel
 */
@Name("storyboardActions")
@Scope(ScopeType.EVENT)
public class StoryboardActions {

    public List<StoryboardItem> getItems(DocumentModel doc) throws PropertyException {
        if (!doc.hasFacet(VideoConstants.HAS_STORYBOARD_FACET)) {
            return Collections.emptyList();
        }
        int size = doc.getProperty(STORYBOARD_PROPERTY).getValue(List.class).size();
        List<StoryboardItem> items = new ArrayList<StoryboardItem>(size);
        for (int i = 0; i < size; i++) {
            items.add(new StoryboardItem(doc, STORYBOARD_PROPERTY, i));
        }
        return items;
    }

    public String getStoryboardItemsAsJsonSettings(DocumentModel doc) throws PropertyException {
        List<StoryboardItem> items = getItems(doc);
        ObjectMapper o = new ObjectMapper();
        ObjectNode settings = o.createObjectNode();
        for (StoryboardItem storyboardItem : items) {
            ObjectNode thumb = o.createObjectNode();
            thumb.put("src", storyboardItem.getUrl());
            settings.put(storyboardItem.getTimecode().split("\\.")[0], thumb);
        }
        return settings.toString();
    }

}
