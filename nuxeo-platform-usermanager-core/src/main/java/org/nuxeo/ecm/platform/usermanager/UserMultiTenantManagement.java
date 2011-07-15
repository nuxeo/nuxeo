/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     bjalon
 */
package org.nuxeo.ecm.platform.usermanager;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Implementations of this interface manages the multi-tenant behavior for
 * UserManager. This class will be used to fetch the User Directory and the
 * Group characteristics
 * 
 * @author bjalon
 */
public interface UserMultiTenantManagement {

    /**
     * Transform filter and fulltext to fetch Groups for the given context and
     * the query specified with the given filter and fulltext. Be careful the
     * filter map and the fulltext set object will be modified so copy them
     * before.
     */
    void queryTransformer(UserManager um, Map<String, Serializable> filter,
            Set<String> fulltext, DocumentModel context) throws ClientException;

    /**
     * Transform the Group DocumentModel store it into the tenant described by
     * the context
     * 
     * @param um
     * @param group to modified
     * @param context that bring the tenant information
     * @throws ClientException
     */
    DocumentModel groupTransformer(UserManager um, DocumentModel group,
            DocumentModel context) throws ClientException;
    
    /**
     * Transform the GroupName to add to tenant characteristic.
     * 
     * @param um
     * @param group to modified
     * @param context that bring the tenant information
     * @throws ClientException
     */
    String groupnameTranformer(UserManager um, String groupname, DocumentModel context);
}
