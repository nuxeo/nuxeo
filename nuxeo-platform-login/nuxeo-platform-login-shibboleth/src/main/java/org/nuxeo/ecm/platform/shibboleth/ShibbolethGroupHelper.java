/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Arnaud Kervern
 */

package org.nuxeo.ecm.platform.shibboleth;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelComparator;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.model.InvalidPropertyValueException;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Reference;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.shibboleth.computedgroups.ELGroupComputerHelper;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.exceptions.GroupAlreadyExistsException;
import org.nuxeo.runtime.api.Framework;

public class ShibbolethGroupHelper {

    private static final Log log = LogFactory.getLog(
            ShibbolethGroupHelper.class);

    protected static DirectoryService directory;

    protected static UserManager userManager;

    private ShibbolethGroupHelper() {
        // Helper class
    }

    protected static DirectoryService getDirectoryService() {
        if (directory == null) {
            try {
                directory = Framework.getService(DirectoryService.class);
            } catch (Exception e) {
                log.error("Failed to open directory service", e);
            }
        }
        return directory;
    }

    protected static UserManager getUserManager() {
        if (userManager == null) {
            try {
                userManager = Framework.getService(UserManager.class);
            } catch (Exception e) {
                log.error("Cannot access the userManager Service");
            }
        }
        return userManager;
    }

    public static DocumentModel getBareGroupModel(CoreSession core)
            throws ClientException {
        return core.createDocumentModel(ShibbolethConstants.SHIBBOLETH_DOCTYPE);
    }

    public static DocumentModel createGroup(DocumentModel group)
            throws ClientException {
        Session session = null;
        try {
            session = getDirectoryService().open(
                    ShibbolethConstants.SHIBBOLETH_DIRECTORY);

            if (session.hasEntry(group.getPropertyValue(
                    ShibbolethConstants.SHIBBOLETH_SCHEMA + ":"
                            + ShibbolethConstants.GROUP_ID_PROPERTY)
                    .toString())) {
                throw new GroupAlreadyExistsException();
            }

            checkExpressionLanguageValidity(group);

            group = session.createEntry(group);
            return group;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public static DocumentModel getGroup(String groupName)
            throws DirectoryException {
        Session session = null;
        try {
            session = getDirectoryService().open(
                    ShibbolethConstants.SHIBBOLETH_DIRECTORY);
            return session.getEntry(groupName);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public static void updateGroup(DocumentModel group) throws ClientException {
        Session session = null;
        try {
            session = getDirectoryService().open(
                    ShibbolethConstants.SHIBBOLETH_DIRECTORY);

            checkExpressionLanguageValidity(group);

            session.updateEntry(group);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public static void deleteGroup(DocumentModel group) throws ClientException {
        Session session = null;
        try {
            session = getDirectoryService().open(
                    ShibbolethConstants.SHIBBOLETH_DIRECTORY);
            session.deleteEntry(group);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    /**
     * Query the group directory to find if shibbGroupName is used in a subGroup
     * field.
     *
     * @param shibbGroupName name of the desired groupe
     * @return a DocumentList representing the groups matching the query
     */
    public static List<String> getParentsGroups(String shibbGroupName)
            throws ClientException {
        Directory dir = getDirectoryService().getDirectory(
                getUserManager().getGroupDirectoryName());

        Reference subGroups = dir.getReference(
                getUserManager().getGroupSubGroupsField());
        List<String> ret = subGroups.getSourceIdsForTarget(shibbGroupName);
        return ret;
    }

    public static DocumentModelList getGroups() throws ClientException {
        Session session = null;
        try {
            session = getDirectoryService().open(
                    ShibbolethConstants.SHIBBOLETH_DIRECTORY);

            return session.getEntries();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    public static DocumentModelList searchGroup(String fullText)
            throws ClientException {
        Session session = null;
        try {
            session = getDirectoryService().open(
                    ShibbolethConstants.SHIBBOLETH_DIRECTORY);

            Map<String, Serializable> filters = new HashMap<String, Serializable>();
            if (fullText != null && !"".equals(fullText)) {
                filters.put(ShibbolethConstants.GROUP_ID_PROPERTY, fullText);
            }

            Map<String, String> orderBy = new HashMap<String, String>();
            orderBy.put(ShibbolethConstants.GROUP_ID_PROPERTY,
                    DocumentModelComparator.ORDER_ASC);
            return session.query(filters, new HashSet<String>(filters.keySet()),
                    orderBy);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    protected static void checkExpressionLanguageValidity(DocumentModel group)
            throws ClientException {
        String expressionLanguage = (String) group.getPropertyValue(
                ShibbolethConstants.SHIBBOLETH_SCHEMA + ":"
                        + ShibbolethConstants.GROUP_EL_PROPERTY);
        if (!ELGroupComputerHelper.isValidEL(expressionLanguage)) {
            throw new InvalidPropertyValueException(expressionLanguage
                    + " : is not a valid expression language");
        }
    }

}
