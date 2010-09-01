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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.documentsLists;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.Base64;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.ui.web.directory.DirectoryHelper;

/**
 * Manage DocumentsLists Persistence.
 * Uses a SQL Directory as storage Backend.
 *
 * @author tiry
 */
public class DocumentsListsPersistenceManager {

    private static final String DIR_NAME = "documentsLists";

    private static final String ID_SEP = ":";

    private static final String DIR_COL_USERID = "userid";

    private static final String DIR_COL_LISTID = "listid";

    private static final String DIR_COL_REF = "ref";

    private static final String DIR_COL_REFTYPE = "reftype";

    private static final String DIR_COL_REPO = "repo";

    // Hey, you never know !
    private static final boolean ENABLE_SANITY_CHECK = true;

    private static final boolean FIX_SANITY_ERROR = true;

    private static final Log log = LogFactory.getLog(DocumentsListsPersistenceManager.class);

    private DirectoryService directoryService;

    private Session dirSession;

    private String directorySchema;

    private static String getIdForEntry(String userName, String listName,
            DocumentModel doc) {
        String ref = doc.getRef().toString();
        int refType = doc.getRef().type();
        String repoId = doc.getRepositoryName();

        return getIdForEntry(userName, listName, ref, refType, repoId);
    }

    private static String getIdForEntry(String userName, String listName, String ref,
            int refType, String repoId) {
        StringBuilder sb = new StringBuilder();
        sb.append(listName);
        sb.append(ID_SEP);
        sb.append(userName);
        sb.append(ID_SEP);
        sb.append(refType);
        sb.append(ID_SEP);
        sb.append(ref);
        sb.append(ID_SEP);
        sb.append(repoId);

        byte[] idDigest;
        try {
            idDigest = MessageDigest.getInstance("MD5").digest(
                    sb.toString().getBytes());
        } catch (NoSuchAlgorithmException e) {
            // should never append
            return sb.toString();
        }
        return Base64.encodeBytes(idDigest);
    }

    private boolean initPersistentService() {
        if (dirSession != null) {
            return true;
        }

        if (directoryService == null) {
            directoryService = DirectoryHelper.getDirectoryService();
            if (directoryService == null) {
                return false;
            }
        }

        try {
            dirSession = directoryService.open(DIR_NAME);
            directorySchema = directoryService.getDirectorySchema(DIR_NAME);
        } catch (ClientException e) {
            dirSession = null;
            log.error("Unable to open directory " + DIR_NAME + " : " + e.getMessage());
            return false;
        }
        return true;
    }

    private void releasePersistenceService() {
        // for now directory sessions are lost during passivation of the DirectoryFacade
        // this can't be tested on the client side
        // => release directorySession after each call ...

        if (directoryService == null) {
            dirSession = null;
            return;
        }
        if (dirSession != null) {
            try {
                dirSession.close();
            } catch (Exception e) {
                // do nothing
            }
        }
        dirSession = null;
    }

    private static DocumentModel getDocModel(CoreSession session, String ref,
            long refType, String repoId) {

        if (!session.getRepositoryName().equals(repoId)) {
            log.error("Multiple repository management is not handled in current implementation");
            return null;
        }
        DocumentRef docRef;
        if (refType == DocumentRef.ID) {
            docRef = new IdRef(ref);
        } else if (refType == DocumentRef.PATH) {
            docRef = new PathRef(ref);
        } else {
            log.error("Unknown reference type");
            return null;
        }

        DocumentModel doc = null;
        try {
            doc = session.getDocument(docRef);
        } catch (ClientException e) {
            log.warn("document with ref " + ref + " was not found : "
                    + e.getMessage());
            return null;
        }

        return doc;
    }

    public List<DocumentModel> loadPersistentDocumentsLists(
            CoreSession currentSession, String userName, String listName) {
        List<DocumentModel> docList = new ArrayList<DocumentModel>();

        if (!initPersistentService()) {
            return docList;
        }

        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        filter.put(DIR_COL_LISTID, listName);
        filter.put(DIR_COL_USERID, userName);

        DocumentModelList entries = null;

        try {
            entries = dirSession.query(filter);
        } catch (DirectoryException e) {
            releasePersistenceService();
            return docList;
        } catch (ClientException e) {
            releasePersistenceService();
            return docList;
        }

        for (DocumentModel entry : entries) {
            String ref;
            long reftype;
            String repo;
            try {
                ref = (String) entry.getProperty(directorySchema,
                        DIR_COL_REF);
                reftype = (Long) entry.getProperty(directorySchema,
                        DIR_COL_REFTYPE);
                repo = (String) entry.getProperty(directorySchema,
                        DIR_COL_REPO);
            } catch (ClientException e1) {
                releasePersistenceService();
                throw new ClientRuntimeException(e1);
            }

            DocumentModel doc = getDocModel(currentSession, ref, reftype, repo);

            if (doc != null) {
                if (ENABLE_SANITY_CHECK) {
                    if (docList.contains(doc)) {
                        log.warn("Document " + doc.getRef().toString()
                                + " is duplicated in persistent list "
                                + listName);
                        if (FIX_SANITY_ERROR) {
                            try {
                                dirSession.deleteEntry(entry.getId());
                            } catch (Exception e) {
                                log.warn("Sanity fix failed " + e.getMessage());
                            }
                        }
                    } else {
                        docList.add(doc);
                    }
                } else {
                    docList.add(doc);
                }
            } else {
                // not found => do the remove
                try {
                    dirSession.deleteEntry(entry.getId());
                } catch (Exception e) {
                    releasePersistenceService();
                    log.error("Unable to remove non existing document model entry : ", e);
                }
            }
        }

        releasePersistenceService();
        return docList;
    }

    public Boolean addDocumentToPersistentList(String userName,
            String listName, DocumentModel doc) {

        if (!initPersistentService()) {
            return false;
        }

        Map<String, Object> fields = new HashMap<String, Object>();
        fields.put(DIR_COL_LISTID, listName);
        fields.put(DIR_COL_USERID, userName);
        fields.put(DIR_COL_REF, doc.getRef().toString());
        fields.put(DIR_COL_REFTYPE, (long) doc.getRef().type());
        fields.put(DIR_COL_REPO, doc.getRepositoryName());
        String id = getIdForEntry(userName, listName, doc);
        fields.put("id", id);

        try {
            if (ENABLE_SANITY_CHECK) {
                DocumentModel badEntry = dirSession.getEntry(id);
                if (badEntry != null) {
                    log.warn("Entry with id " + id
                            + " is already present : please check DB integrity");
                    if (FIX_SANITY_ERROR) {
                        dirSession.deleteEntry(id);
                    }
                }
            }
            dirSession.createEntry(fields);
        } catch (Exception e) {
            log.error("Unable to create entry : " + e.getMessage());
            releasePersistenceService();
            return false;
        }
        releasePersistenceService();

        return true;
    }

    public Boolean removeDocumentFromPersistentList(String userName,
            String listName, DocumentModel doc) {

        if (!initPersistentService()) {
            return false;
        }

        String entryId = getIdForEntry(userName, listName, doc);

        try {
            dirSession.deleteEntry(entryId);
        } catch (Exception e) {
            releasePersistenceService();
            log.error("Unable to delete entry : " + e.getMessage());
            return false;
        }

        releasePersistenceService();
        return true;
    }

    public Boolean clearPersistentList(String userName, String listName) {
        if (!initPersistentService()) {
            return false;
        }

        Map<String, Serializable> filter = new HashMap<String, Serializable>();
        filter.put(DIR_COL_LISTID, listName);
        filter.put(DIR_COL_USERID, userName);

        try {
            DocumentModelList entriesToDelete = dirSession.query(filter);

            for (DocumentModel entry : entriesToDelete) {
                dirSession.deleteEntry(entry.getId());
            }
        } catch (Exception e) {
            log.error("Unable to clear DocumentList : " + e.getMessage());
            releasePersistenceService();
            return false;
        }
        releasePersistenceService();
        return true;
    }

}
