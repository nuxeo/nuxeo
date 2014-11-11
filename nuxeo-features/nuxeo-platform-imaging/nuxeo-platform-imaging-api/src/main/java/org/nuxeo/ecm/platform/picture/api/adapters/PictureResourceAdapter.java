/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.picture.api.adapters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.PropertyException;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;

public interface PictureResourceAdapter {

    void setDocumentModel(DocumentModel doc);

    /**
     * @deprecated since 5.7. Use
     *             {@link #fillPictureViews(org.nuxeo.ecm.core.api.Blob, String, String, java.util.ArrayList)}
     *             instead.
     */
    @Deprecated
    boolean createPicture(Blob fileContent, String filename, String title,
            ArrayList<Map<String, Object>> pictureTemplates)
            throws IOException, ClientException;

    /**
     * Fill this Picture views using the given {@code pictureTemplates} and
     * {@code blob} to compute the picture views.
     * <p>
     * The {@code blob} is converted to fit the defined {@code pictureTemplates}.
     *
     * @since 5.7
     */
    boolean fillPictureViews(Blob blob, String filename, String title,
            ArrayList<Map<String, Object>> pictureTemplates)
            throws IOException, ClientException;

    /**
     * Pre-fill this Picture views using the given {@code pictureTemplates} and
     * {@code blob}.
     * <p>
     * The {@code blob} is not converted and just stored as the Blob of the
     * picture views.
     *
     * @since 5.7
     */
    void preFillPictureViews(Blob blob,
            List<Map<String, Object>> pictureTemplates, ImageInfo imageInfo)
            throws IOException, ClientException;

    void doRotate(int angle) throws ClientException;

    void doCrop(String coords) throws ClientException;

    Blob getPictureFromTitle(String title) throws PropertyException,
            ClientException;

    /**
     * Returns the XPath of the given view name, or {@code null} if the view is
     * not found on the Picture.
     *
     * @param viewName the view name
     */
    String getViewXPath(String viewName);

    /**
     * Convenience method to get the XPath of the first view of the Picture.
     *
     * @return the XPath of the first view
     */
    String getFirstViewXPath();

}
