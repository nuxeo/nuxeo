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

import static org.nuxeo.ecm.directory.localconfiguration.DirectoryConfigurationConstants.DIRECTORY_CONFIGURATION_FACET;

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.localconfiguration.LocalConfigurationService;
import org.nuxeo.ecm.directory.localconfiguration.DirectoryConfiguration;
import org.nuxeo.runtime.api.Framework;

/**
 * @author bjalon
 * 
 */
public class DefaultUserMultiTenantManagement implements
        UserMultiTenantManagement {

    protected static final Log log = LogFactory.getLog(DefaultUserMultiTenantManagement.class);

    protected static final String SUFFIX_SEPARATOR = "-";

    protected String getDirectorySuffix(DocumentModel documentContext) {
        DirectoryConfiguration configuration = null;

        try {
            LocalConfigurationService localConfigurationService = Framework.getService(LocalConfigurationService.class);

            configuration = localConfigurationService.getConfiguration(
                    DirectoryConfiguration.class,
                    DIRECTORY_CONFIGURATION_FACET, documentContext);
        } catch (Exception e) {
            log.error(e, e);
        }

        if (configuration != null && configuration.getDirectorySuffix() != null) {
            return SUFFIX_SEPARATOR + configuration.getDirectorySuffix();
        }

        return null;
    }

    @Override
    public void queryTransformer(UserManager um,
            Map<String, Serializable> filter, Set<String> fulltext,
            DocumentModel context) throws ClientException {
        String groupId = um.getGroupIdField();
        if (filter == null || fulltext == null) {
            throw new ClientException(
                    "Filter and Fulltext must be not null, please check");
        }

        if (getDirectorySuffix(context) == null) {
            log.debug("Directory Local Configuration is null, don't need to filter");
            return;
        }

        String groupIdSuffix = getDirectorySuffix(context);

        if (!filter.containsKey(groupId)) {
            log.debug("no filter on group id, need to filter with the directory local "
                    + "configuration suffix : "
                    + groupId
                    + " = %"
                    + groupIdSuffix);
            filter.put(groupId, "%" + groupIdSuffix);
            fulltext.add(groupId);
            return;
        }

        if (!(filter.get(groupId) instanceof String)) {
            throw new UnsupportedOperationException("Filter value on "
                    + "group id is not a string : " + filter.get(groupId));
        }

        String filterIdValue = (String) filter.get(um.getGroupIdField());
        filter.put(groupId, filterIdValue + groupIdSuffix);
    }

    @Override
    public DocumentModel groupTransformer(UserManager um,
            DocumentModel group, DocumentModel context) throws ClientException {
        if (context == null) {
            return group;
        }
        String groupIdValue = group.getPropertyValue(um.getGroupIdField())
                + getDirectorySuffix(context);
        group.setPropertyValue(um.getGroupIdField(), groupIdValue);
        return group;
    }

    @Override
    public String groupnameTranformer(UserManager um, String groupname, DocumentModel context) {
        String suffix = getDirectorySuffix(context);
        if (suffix != null) {
            groupname += suffix;
        }
        return groupname;
    }
}
