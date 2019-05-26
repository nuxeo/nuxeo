/*
 * (C) Copyright 2006-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 */
package org.nuxeo.ecm.webapp.documentsLists;

import java.io.Serializable;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentNotFoundException;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentSecurityException;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.ui.web.directory.DirectoryHelper;

/**
 * Manage DocumentsLists Persistence. Uses a SQL Directory as storage Backend.
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

    private static String getIdForEntry(String userName, String listName, DocumentModel doc) {
        String ref = doc.getRef().toString();
        int refType = doc.getRef().type();
        String repoId = doc.getRepositoryName();

        return getIdForEntry(userName, listName, ref, refType, repoId);
    }

    private static String getIdForEntry(String userName, String listName, String ref, int refType, String repoId) {
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
            idDigest = MessageDigest.getInstance("MD5").digest(sb.toString().getBytes());
        } catch (NoSuchAlgorithmException e) {
            // should never append
            return sb.toString();
        }
        return Base64.encodeBase64String(idDigest);
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
        } catch (DirectoryException e) {
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
            } catch (DirectoryException e) {
                // do nothing
            }
        }
        dirSession = null;
    }

    private static DocumentModel getDocModel(CoreSession session, String ref, long refType, String repoId) {

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

        try {
            return session.getDocument(docRef);
        } catch (DocumentSecurityException | DocumentNotFoundException e) {
            log.warn("document with ref " + ref + " was not found : " + e.getMessage());
            return null;
        }
    }

    public List<DocumentModel> loadPersistentDocumentsLists(CoreSession currentSession, String userName,
            String listName) {
        List<DocumentModel> docList = new ArrayList<>();
        if (!initPersistentService()) {
            return docList;
        }
        try {
            Map<String, Serializable> filter = new HashMap<>();
            filter.put(DIR_COL_LISTID, listName);
            filter.put(DIR_COL_USERID, userName);

            DocumentModelList entries;
            try {
                entries = dirSession.query(filter);
            } catch (DirectoryException e) {
                log.error(e, e);
                return docList;
            }

            for (DocumentModel entry : entries) {
                String ref = (String) entry.getProperty(directorySchema, DIR_COL_REF);
                long reftype = (Long) entry.getProperty(directorySchema, DIR_COL_REFTYPE);
                String repo = (String) entry.getProperty(directorySchema, DIR_COL_REPO);

                DocumentModel doc = getDocModel(currentSession, ref, reftype, repo);

                if (doc != null) {
                    if (ENABLE_SANITY_CHECK) {
                        if (docList.contains(doc)) {
                            log.warn("Document " + doc.getRef().toString() + " is duplicated in persistent list "
                                    + listName);
                            if (FIX_SANITY_ERROR) {
                                try {
                                    dirSession.deleteEntry(entry.getId());
                                } catch (DirectoryException e) {
                                    log.error("Sanity fix failed", e);
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
                    } catch (DirectoryException e) {
                        log.error("Unable to remove non existing document model entry : " + entry.getId(), e);
                    }
                }
            }
            return docList;
        } finally {
            releasePersistenceService();
        }
    }

    public Boolean addDocumentToPersistentList(String userName, String listName, DocumentModel doc) {
        if (!initPersistentService()) {
            return false;
        }
        try {
            Map<String, Object> fields = new HashMap<>();
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
                        log.warn("Entry with id " + id + " is already present : please check DB integrity");
                        if (FIX_SANITY_ERROR) {
                            dirSession.deleteEntry(id);
                        }
                    }
                }
                dirSession.createEntry(fields);
            } catch (DirectoryException e) {
                log.error("Unable to create entry", e);
                return false;
            }
            return true;
        } finally {
            releasePersistenceService();
        }
    }

    public boolean removeDocumentFromPersistentList(String userName, String listName, DocumentModel doc) {
        if (!initPersistentService()) {
            return false;
        }
        try {
            String entryId = getIdForEntry(userName, listName, doc);
            try {
                dirSession.deleteEntry(entryId);
            } catch (DirectoryException e) {
                log.error("Unable to delete entry", e);
                return false;
            }
            return true;
        } finally {
            releasePersistenceService();
        }
    }

    public boolean clearPersistentList(String userName, String listName) {
        if (!initPersistentService()) {
            return false;
        }
        try {
            Map<String, Serializable> filter = new HashMap<>();
            filter.put(DIR_COL_LISTID, listName);
            filter.put(DIR_COL_USERID, userName);
            try {
                DocumentModelList entriesToDelete = dirSession.query(filter);
                for (DocumentModel entry : entriesToDelete) {
                    dirSession.deleteEntry(entry.getId());
                }
            } catch (DirectoryException e) {
                log.error("Unable to clear DocumentList", e);
                return false;
            }
            return true;
        } finally {
            releasePersistenceService();
        }
    }

}
