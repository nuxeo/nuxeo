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

    String ALL = "all";

    String VALID_CHARS = "0123456789_-"
            + "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    @Factory(value = "groupList", scope = EVENT)
    DocumentModelList getGroups() throws ClientException;

    void resetGroups();

    String viewGroups();

    String viewGroup() throws ClientException;

    String viewGroup(String groupName) throws ClientException;

    String editGroup() throws ClientException;

    DocumentModel getSelectedGroup();

    DocumentModel getNewGroup() throws ClientException;

    String deleteGroup() throws ClientException;

    String updateGroup() throws ClientException;

    void validateGroupName(FacesContext context, UIComponent component,
            Object value);

    String createGroup() throws ClientException;

    boolean getAllowCreateGroup() throws ClientException;

    boolean getAllowDeleteGroup() throws ClientException;

    boolean getAllowEditGroup() throws ClientException;

    String getSearchString();

    void setSearchString(String searchString);

    String searchGroups();

    String clearSearch();

    boolean isSearchOverflow();

    boolean isSelectedGroupReadOnly();

}
