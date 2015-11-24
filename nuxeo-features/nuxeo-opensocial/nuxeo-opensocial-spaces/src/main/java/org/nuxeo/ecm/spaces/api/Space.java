/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Leroy Merlin (http://www.leroymerlin.fr/) - initial implementation
 */

package org.nuxeo.ecm.spaces.api;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.opensocial.container.server.layout.YUILayoutAdapter;
import org.nuxeo.opensocial.container.shared.layout.api.YUILayout;
import org.nuxeo.opensocial.container.shared.webcontent.WebContentData;

public interface Space extends SimplePermissionMapper {

    String getId();

    String getName();

    String getTitle() throws ClientException;

    void setTitle(String title) throws ClientException;

    String getDescription() throws ClientException;

    void setDescription(String description) throws ClientException;

    YUILayoutAdapter getLayout() throws ClientException;

    /**
     * Initialize the layout be resetting it. Implementations MAY although reset
     * content (that will be lost)
     *
     * @param layout
     * @throws org.nuxeo.ecm.core.api.ClientException
     */
    void initLayout(YUILayout layout) throws ClientException;

    /**
     * Family/category of this space
     *
     * @return
     */
    String getCategory() throws ClientException;

    void setCategory(String category) throws ClientException;

    String getOwner() throws ClientException;

    String getViewer() throws ClientException;

    boolean isReadOnly() throws ClientException;

    WebContentData createWebContent(WebContentData data) throws ClientException;

    List<WebContentData> readWebContents() throws ClientException;

    WebContentData updateWebContent(WebContentData data) throws ClientException;

    void deleteWebContent(WebContentData data) throws ClientException;

    void save() throws ClientException;

    void remove() throws ClientException;

    Space copyFrom(Space space) throws ClientException;

    WebContentData getWebContent(String webContentId) throws ClientException;

    void moveWebContent(WebContentData data, String unitId)
            throws ClientException;

}
