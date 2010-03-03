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
import java.util.Calendar;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;

public interface Space extends Comparable<Space> {

    /**
     * Unique identifier of a space instance
     * 
     * @return
     */
    String getId();

    /**
     * Name of the space
     * 
     * @return
     */
    String getName();

    /**
     * Space theme
     * 
     * @return
     */
    String getTheme() throws ClientException;

    void setTheme(String theme) throws ClientException;

    /**
     * Title of the space
     * 
     * @return
     */
    String getTitle() throws ClientException;

    void setTitle(String title) throws ClientException;

    /**
     * description of the space
     * 
     * @return
     */
    String getDescription() throws ClientException;

    void setDescription(String description) throws ClientException;

    /**
     * A key for displaying elements in this space
     * 
     * @return
     */
    String getLayout() throws ClientException;

    void setLayout(String name) throws ClientException;

    /**
     * Family/category of this space
     * 
     * @return
     */
    String getCategory() throws ClientException;

    void setCategory(String category) throws ClientException;

    /**
     * Name of the creator of this space
     * 
     * @return
     */
    String getOwner() throws ClientException;

    /**
     * Name of the viewer of this space
     * 
     * @return
     */
    String getViewer() throws ClientException;

    boolean isReadOnly() throws ClientException;

    Gadget createGadget(String gadgetName) throws ClientException;

    Gadget createGadget(URL gadgetDefUrl) throws ClientException;

    void save(Gadget gadget) throws ClientException;

    void remove(Gadget gadget) throws ClientException;

    List<Gadget> getGadgets() throws ClientException;

    Gadget getGadget(String id) throws ClientException;

    boolean hasPermission(String permissionName) throws ClientException;

    void save() throws ClientException;

    void remove() throws ClientException;

    Space copyFrom(Space space) throws ClientException;

    Calendar getPublicationDate() throws ClientException;

    void setPublicationDate(Calendar datePublication) throws ClientException;

    public List<String> getPermissions() throws Exception;

}
