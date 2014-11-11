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

import java.util.List;

import javax.ejb.Local;
import javax.faces.model.SelectItem;

import org.nuxeo.ecm.core.api.ClientException;

/**
 * Provides user manager related operations.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
@Local
public interface GroupManagerActions {

    void initialize() throws ClientException;

    String createGroup() throws ClientException;

    String deleteGroup() throws ClientException;

    void destroy();

    void recomputeGroupList() throws ClientException;

    String editGroup() throws ClientException;

    String viewGroup() throws ClientException;

    String updateGroup() throws ClientException;

    String viewGroup(String groupName) throws ClientException;

    String saveGroup() throws ClientException;

    List<SelectItem> getAvailableGroups() throws ClientException;

    boolean getAllowCreateGroup() throws ClientException;

    boolean getAllowDeleteGroup() throws ClientException;

    boolean getAllowEditGroup() throws ClientException;

    String getSearchString() throws ClientException;

    void setSearchString(String searchString) throws ClientException;

    String searchGroups() throws ClientException;

    String clearSearch() throws ClientException;

    boolean isSearchOverflow();

}
