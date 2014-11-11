/*
 * (C) Copyright 2006-2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     tmartins
 */
package org.nuxeo.ecm.platform.usermanager;

import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;

/**
* Interface to expose user model fields
* @since 5.7
*
* @author <a href="mailto:tm@nuxeo.com">Thierry Martins</a>
*/
public interface UserAdapter {

    String getName() throws ClientException;

    String getFirstName() throws ClientException;

    String getLastName() throws ClientException;

    String getEmail() throws ClientException;

    String getCompany() throws ClientException;

    List<String> getGroups() throws ClientException;

    String getSchemaName() throws ClientException;
}
