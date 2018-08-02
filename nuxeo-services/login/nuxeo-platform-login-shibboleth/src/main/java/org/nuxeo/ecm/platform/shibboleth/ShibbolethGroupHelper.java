/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Arnaud Kervern
 */

package org.nuxeo.ecm.platform.shibboleth;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelComparator;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.model.InvalidPropertyValueException;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Reference;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.shibboleth.computedgroups.ELGroupComputerHelper;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.platform.usermanager.exceptions.GroupAlreadyExistsException;
import org.nuxeo.runtime.api.Framework;

public class ShibbolethGroupHelper {

    private ShibbolethGroupHelper() {
        // Helper class
    }

    protected static DirectoryService getDirectoryService() {
        return Framework.getService(DirectoryService.class);
    }

    protected static UserManager getUserManager() {
        return Framework.getService(UserManager.class);
    }

    public static DocumentModel getBareGroupModel(CoreSession core) {
        return core.createDocumentModel(ShibbolethConstants.SHIBBOLETH_DOCTYPE);
    }

    public static DocumentModel createGroup(DocumentModel group) {
        try (Session session = getDirectoryService().open(ShibbolethConstants.SHIBBOLETH_DIRECTORY)) {
            if (session.hasEntry(group.getPropertyValue(
                    ShibbolethConstants.SHIBBOLETH_SCHEMA + ":" + ShibbolethConstants.GROUP_ID_PROPERTY).toString())) {
                throw new GroupAlreadyExistsException();
            }

            checkExpressionLanguageValidity(group);

            group = session.createEntry(group);
            return group;
        }
    }

    public static DocumentModel getGroup(String groupName) {
        try (Session session = getDirectoryService().open(ShibbolethConstants.SHIBBOLETH_DIRECTORY)) {
            return session.getEntry(groupName);
        }
    }

    public static void updateGroup(DocumentModel group) {
        try (Session session = getDirectoryService().open(ShibbolethConstants.SHIBBOLETH_DIRECTORY)) {
            checkExpressionLanguageValidity(group);

            session.updateEntry(group);
        }
    }

    public static void deleteGroup(DocumentModel group) {
        try (Session session = getDirectoryService().open(ShibbolethConstants.SHIBBOLETH_DIRECTORY)) {
            session.deleteEntry(group);
        }
    }

    /**
     * Query the group directory to find if shibbGroupName is used in a subGroup field.
     *
     * @param shibbGroupName name of the desired groupe
     * @return a DocumentList representing the groups matching the query
     */
    public static List<String> getParentsGroups(String shibbGroupName) {
        Directory dir = getDirectoryService().getDirectory(getUserManager().getGroupDirectoryName());

        Reference subGroups = dir.getReference(getUserManager().getGroupSubGroupsField());
        List<String> ret = subGroups.getSourceIdsForTarget(shibbGroupName);
        return ret;
    }

    public static DocumentModelList getGroups() {
        try (Session session = getDirectoryService().open(ShibbolethConstants.SHIBBOLETH_DIRECTORY)) {
            return session.getEntries();
        }
    }

    public static DocumentModelList searchGroup(String fullText) {
        try (Session session = getDirectoryService().open(ShibbolethConstants.SHIBBOLETH_DIRECTORY)) {
            Map<String, Serializable> filters = new HashMap<String, Serializable>();
            if (fullText != null && !"".equals(fullText)) {
                filters.put(ShibbolethConstants.GROUP_ID_PROPERTY, fullText);
            }

            Map<String, String> orderBy = new HashMap<String, String>();
            orderBy.put(ShibbolethConstants.GROUP_ID_PROPERTY, DocumentModelComparator.ORDER_ASC);
            return session.query(filters, new HashSet<String>(filters.keySet()), orderBy);
        }
    }

    protected static void checkExpressionLanguageValidity(DocumentModel group) {
        String expressionLanguage = (String) group.getPropertyValue(ShibbolethConstants.SHIBBOLETH_SCHEMA + ":"
                + ShibbolethConstants.GROUP_EL_PROPERTY);
        if (!ELGroupComputerHelper.isValidEL(expressionLanguage)) {
            throw new InvalidPropertyValueException(expressionLanguage + " : is not a valid expression language");
        }
    }

}
