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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;

/**
 * Small DTO to precompute the thumbnail URLs for JSF
 */
public class StoryboardItem {

    public static final Log log = LogFactory.getLog(StoryboardItem.class);

    protected final DocumentModel doc;

    protected final int position;

    protected final String blobPropertyName;

    protected final String filename;

    protected String timecode = "0";

    public StoryboardItem(DocumentModel doc, String basePropertyPath, int position) {
        this.doc = doc;
        this.position = position;
        String propertyPath = basePropertyPath + "/" + position;
        blobPropertyName = propertyPath + "/content";
        filename = String.format("storyboard-%03d.jpeg", position);
        try {
            Double tc = (Double) doc.getPropertyValue(propertyPath + "/timecode");
            if (tc != null) {
                timecode = String.format("%.2f", tc);
            }
            // TODO: read filename from blob too
        } catch (PropertyException e) {
            log.warn(e);
        }
    }

    public String getUrl() {
        return DocumentModelFunctions.bigFileUrl(doc, blobPropertyName, filename);
    }

    public String getTimecode() {
        return timecode;
    }
}
