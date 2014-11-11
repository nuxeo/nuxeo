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

import java.net.URL;
import java.util.Map;

import org.nuxeo.ecm.core.api.ClientException;

public interface Gadget {

    /**
     * Unique identifier of a gadget
     *
     * @return
     */
    String getId();

    /**
     * name
     *
     * @return
     */
    String getName() throws ClientException;

    void setName(String name) throws ClientException;

    URL getDefinitionUrl() throws ClientException;

    void setDefinitionUrl(URL url) throws ClientException;

    /**
     * description
     *
     * @return
     */
    String getDescription() throws ClientException;

    void setDescription(String description) throws ClientException;

    /**
     * title
     *
     * @return
     */
    String getTitle() throws ClientException;

    void setTitle(String title) throws ClientException;

    /**
     * creator name
     *
     * @return
     */
    String getOwner() throws ClientException;

    String getViewer() throws ClientException;

    /**
     * category of a gadget
     *
     * @return
     */
    String getCategory() throws ClientException;

    void setCategory(String category) throws ClientException;

    /**
     * preferences values
     *
     * @return
     */
    Map<String, String> getPreferences() throws ClientException;

    void setPreferences(Map<String, String> prefs) throws ClientException;

    String getPref(String prefKey) throws ClientException;

    /**
     * Key corresponding to the place where the gadget will be positionned in
     * the view
     *
     * @return
     */
    String getPlaceId() throws ClientException;

    void setPlaceId(String placeId) throws ClientException;

    /**
     * Relative position in the parent container at the place id "getPlaceID()"
     *
     * @return
     */
    int getPosition() throws ClientException;

    public void setPosition(int position) throws ClientException;

    /**
     * Determines if the display state of the gadget
     *
     * @return
     */
    boolean isCollapsed() throws ClientException;

    void setCollapsed(boolean collapsed) throws ClientException;

    boolean isEqualTo(Gadget gadget) throws ClientException;

    Space getParent() throws ClientException;

    int getHeight() throws ClientException;

    void setHeight(int height) throws ClientException;

    void copyFrom(Gadget gadget) throws ClientException;

    void save() throws ClientException;

    boolean isEditable() throws ClientException;

    boolean isConfigurable() throws ClientException;

}
