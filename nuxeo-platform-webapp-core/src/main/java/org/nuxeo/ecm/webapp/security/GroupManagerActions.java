/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.webapp.security;

import static org.jboss.seam.ScopeType.EVENT;

import java.io.Serializable;

import javax.ejb.Local;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.jboss.seam.annotations.Factory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * Provides user manager related operations.
 *
 * @author Razvan Caraghin
 */
@Local
public interface GroupManagerActions extends Serializable {

    public static final String ALL = "all";

    public static final String VALID_CHARS = "0123456789_-"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    @Factory(value = "groupList", scope = EVENT)
    public DocumentModelList getGroups() throws ClientException;

    public void resetGroups() throws ClientException;

    public String viewGroups() throws ClientException;

    public String viewGroup() throws ClientException;

    public String viewGroup(String groupName) throws ClientException;

    public String editGroup() throws ClientException;

    public DocumentModel getSelectedGroup() throws ClientException;

    public DocumentModel getNewGroup() throws ClientException;

    public String deleteGroup() throws ClientException;

    public String updateGroup() throws ClientException;

    public void validateGroupName(FacesContext context, UIComponent component,
            Object value);

    public String createGroup() throws ClientException;

    public boolean getAllowCreateGroup() throws ClientException;

    public boolean getAllowDeleteGroup() throws ClientException;

    public boolean getAllowEditGroup() throws ClientException;

    public String getSearchString();

    public void setSearchString(String searchString);

    public String searchGroups() throws ClientException;

    public String clearSearch() throws ClientException;

    public boolean isSearchOverflow();

}
