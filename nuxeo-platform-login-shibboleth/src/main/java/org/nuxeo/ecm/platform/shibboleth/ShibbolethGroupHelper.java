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
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelComparator;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.model.InvalidPropertyValueException;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.computedgroups.ELGroupComputerHelper;
import org.nuxeo.ecm.platform.usermanager.exceptions.GroupAlreadyExistsException;
import org.nuxeo.runtime.api.Framework;

public class ShibbolethGroupHelper {

    private static final Log log = LogFactory.getLog(ShibbolethGroupHelper.class);

    protected static DirectoryService directory;

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

    public static DocumentModel getBareGroupModel(CoreSession core)
            throws ClientException {
        return core.createDocumentModel(ShibbolethConstants.SHIBBOLETH_SCHEMA);
    }

    public static DocumentModel createGroup(DocumentModel group)
            throws ClientException {
        Session session = null;
        try {
            session = getDirectoryService().open(
                    ShibbolethConstants.SHIBBOLETH_DIRECTORY);

            if (session.hasEntry(group.getPropertyValue(
                    ShibbolethConstants.SHIBBOLETH_SCHEMA + ":"
                            + ShibbolethConstants.GROUP_ID_PROPERTY).toString())) {
                throw new GroupAlreadyExistsException();
            }

            checkExpressionLanguageValidity(group);

            group = session.createEntry(group);
            session.commit();
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
            session.commit();
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
            session.commit();
        } finally {
            if (session != null) {
                session.close();
            }
        }
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
            if (fullText != null && fullText != "") {
                filters.put(ShibbolethConstants.GROUP_ID_PROPERTY, fullText);
            }

            Map<String, String> orderBy = new HashMap<String, String>();
            orderBy.put(ShibbolethConstants.GROUP_ID_PROPERTY,
                    DocumentModelComparator.ORDER_ASC);
            return session.query(filters,
                    new HashSet<String>(filters.keySet()), orderBy);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    protected static void checkExpressionLanguageValidity(DocumentModel group)
            throws ClientException {
        String expressionLanguage = (String) group.getPropertyValue(ShibbolethConstants.SHIBBOLETH_SCHEMA
                + ":" + ShibbolethConstants.GROUP_EL_PROPERTY);
        if (!ELGroupComputerHelper.isValidEL(expressionLanguage)) {
            throw new InvalidPropertyValueException(expressionLanguage
                    + " : is not a valid expression language");
        }
    }
}
