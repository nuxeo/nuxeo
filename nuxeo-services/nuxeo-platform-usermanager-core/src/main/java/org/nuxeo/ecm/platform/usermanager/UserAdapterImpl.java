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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * A class that exposes the fields from user schema
 *
 * @since 5.7
 * @author <a href="mailto:tm@nuxeo.com">Thierry Martins</a>
 */
public class UserAdapterImpl implements UserAdapter {

    private static final Log log = LogFactory.getLog(UserAdapterImpl.class);

    protected final DocumentModel doc;

    protected final UserConfig userConfig;

    private DataModel dataModel;

    public UserAdapterImpl(DocumentModel doc, UserManager userManager) {
        this.doc = doc;
        if (userManager != null && userManager instanceof UserManagerImpl) {
            userConfig = ((UserManagerImpl) userManager).userConfig;
        } else {
            userConfig = UserConfig.DEFAULT;
        }
        dataModel = doc.getDataModel(userConfig.schemaName);
    }

    @Override
    public String getName() {
        return (String) dataModel.getValue(userConfig.nameKey);
    }

    @Override
    public String getFirstName() {
        return (String) dataModel.getValue(userConfig.firstNameKey);
    }

    @Override
    public String getLastName() {
        return (String) dataModel.getValue(userConfig.lastNameKey);
    }

    @Override
    public String getEmail() {
        return (String) dataModel.getValue(userConfig.emailKey);
    }

    @Override
    public String getCompany() {
        return (String) dataModel.getValue(userConfig.companyKey);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getGroups() {
        return (List<String>) dataModel.getValue(userConfig.groupsKey);
    }

    /**
     * @since 8.1
     */
    public String getTenantId() {
        return (String) dataModel.getValue(userConfig.tenantIdKey);
    }

    @Override
    public String getSchemaName() {
        return userConfig.schemaName;
    }
}
