/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    @Override
    public String getTenantId() {
        return (String) dataModel.getValue(userConfig.tenantIdKey);
    }

    @Override
    public String getSchemaName() {
        return userConfig.schemaName;
    }
}
