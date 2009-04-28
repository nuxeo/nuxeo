/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     troger
 */
package org.nuxeo.ecm.platform.picture.preview.helper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;

/**
 *@author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public final class PicturePreviewHelper {

    private static Log log = LogFactory.getLog(PicturePreviewHelper.class);

    private static final String VIEWS_PROPERTY = "picture:views";

    private static final String TITLE_PROPERTY = "title";

    private static final String ORIGINAL_VIEW_TITLE = "Original";

    private static final String BLOB_XPATH = "picture:views/item[%d]/";

    private PicturePreviewHelper() {
        // Helper class
    }

    /**
     *  Returns the xpath of the Original view of the given Picture,
     *  or {@code null} of it can't compute it.
     *
     * @param doc the Picture document
     */
    public static String getOriginalViewXPath(DocumentModel doc) {
        try {
            Property views = doc.getProperty(VIEWS_PROPERTY);
            for (int i = 0; i < views.size(); i++) {
                if (views.get(i).getValue(TITLE_PROPERTY).equals(ORIGINAL_VIEW_TITLE)) {
                    return getViewXPathFor(i);
                }
            }
        } catch (ClientException e) {
            log.error("Unable to get picture views", e);
        }
        return null;
    }

    public static String getViewXPathFor(int index) {
        return String.format(BLOB_XPATH, index);
    }

}
