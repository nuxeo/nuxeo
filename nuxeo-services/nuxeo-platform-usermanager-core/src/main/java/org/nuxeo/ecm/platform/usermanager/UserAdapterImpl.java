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
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DataModel;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.DataModelImpl;

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
        try {
            dataModel = doc.getDataModel(userConfig.schemaName);
        } catch (ClientException e) {
            log.error(
                    "Unable to get data model for schema " + userConfig.schemaName + ". Building an empty data model",
                    e);
            // empty data model to avoid error
            dataModel = new DataModelImpl(userConfig.schemaName);
        }
    }

    public UserAdapterImpl(DocumentModel doc, UserConfig userConfig) {
        this.doc = doc;
        if (userConfig != null) {
            this.userConfig = userConfig;
        } else {
            this.userConfig = UserConfig.DEFAULT;
        }
        try {
            dataModel = doc.getDataModel(userConfig.schemaName);
        } catch (ClientException e) {
            log.error(
                    "Unable to get data model for schema " + userConfig.schemaName + ". Building an empty data model",
                    e);
            // empty data model to avoid error
            dataModel = new DataModelImpl(userConfig.schemaName);
        }
    }

    @Override
    public String getName() throws ClientException {
        return (String) dataModel.getValue(userConfig.nameKey);
    }

    @Override
    public String getFirstName() throws ClientException {
        return (String) dataModel.getValue(userConfig.firstNameKey);
    }

    @Override
    public String getLastName() throws ClientException {
        return (String) dataModel.getValue(userConfig.lastNameKey);
    }

    @Override
    public String getEmail() throws ClientException {
        return (String) dataModel.getValue(userConfig.emailKey);
    }

    @Override
    public String getCompany() throws ClientException {
        return (String) dataModel.getValue(userConfig.companyKey);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getGroups() throws ClientException {
        return (List<String>) dataModel.getValue(userConfig.groupsKey);
    }

    @Override
    public String getSchemaName() throws ClientException {
        return userConfig.schemaName;
    }

    @Override
    public UserConfig getConfig() {
        return userConfig.clone();
    }
}
