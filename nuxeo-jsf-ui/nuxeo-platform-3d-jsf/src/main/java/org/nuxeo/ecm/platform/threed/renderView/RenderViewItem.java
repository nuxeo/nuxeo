/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Tiago Cardoso <tcardoso@nuxeo.com>
 */
package org.nuxeo.ecm.platform.threed.renderView;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PropertyException;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;

/**
 * Small DTO to precompute the thumbnail URLs for JSF
 *
 * @since 8.4
 */
public class RenderViewItem {

    public static final Log log = LogFactory.getLog(RenderViewItem.class);

    protected final DocumentModel doc;

    protected final int position;

    protected final String blobPropertyName;

    protected final String thumbnailPropertyName;

    protected final String titlePropertyName;

    protected String filename;

    protected String thumbFilename;

    protected String title;

    public RenderViewItem(DocumentModel doc, String basePropertyPath, int position) {
        this.doc = doc;
        this.position = position;
        String propertyPath = basePropertyPath + "/" + position;
        blobPropertyName = propertyPath + "/content";
        thumbnailPropertyName = propertyPath + "/thumbnail";
        titlePropertyName = propertyPath + "/title";
        try {
            filename = ((Blob) doc.getPropertyValue(blobPropertyName)).getFilename();
            thumbFilename = ((Blob) doc.getPropertyValue(blobPropertyName)).getFilename();
            title = (String) doc.getPropertyValue(titlePropertyName);
        } catch (PropertyException e) {
            log.warn(e);
        }
    }

    public String getThumbnailUrl() {
        return DocumentModelFunctions.bigFileUrl(doc, thumbnailPropertyName, thumbFilename);
    }

    public String getUrl() {
        return DocumentModelFunctions.bigFileUrl(doc, blobPropertyName, filename);
    }

    public String getTitle() {
        return title;
    }

    public Integer getAzimuth() {
        return Integer.valueOf(thumbFilename.split("-")[3]);
    }

    public Integer getZenith() {
        return Integer.valueOf(thumbFilename.split("-")[4]);
    }

}
