/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */
package org.nuxeo.apidoc.documentation;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.apidoc.api.DocumentationItem;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.apidoc.api.QueryHelper;
import org.nuxeo.apidoc.search.ArtifactSearcher;
import org.nuxeo.apidoc.search.ArtifactSearcherImpl;
import org.nuxeo.apidoc.security.SecurityConstants;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.io.DocumentPipe;
import org.nuxeo.ecm.core.io.DocumentReader;
import org.nuxeo.ecm.core.io.DocumentTransformer;
import org.nuxeo.ecm.core.io.DocumentWriter;
import org.nuxeo.ecm.core.io.ExportedDocument;
import org.nuxeo.ecm.core.io.impl.DocumentPipeImpl;
import org.nuxeo.ecm.core.io.impl.plugins.DocumentModelWriter;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveReader;
import org.nuxeo.ecm.core.io.impl.plugins.NuxeoArchiveWriter;
import org.nuxeo.ecm.core.query.sql.NXQL;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

public class DocumentationComponent extends DefaultComponent implements DocumentationService {

    public static final String DIRECTORY_NAME = "documentationTypes";

    public static final String Root_PATH = "/";

    public static final String Root_NAME = "nuxeo-api-doc";

    public static final String Read_Grp = SecurityConstants.Read_Group;

    public static final String Write_Grp = SecurityConstants.Write_Group;

    protected static final Log log = LogFactory.getLog(DocumentationComponent.class);

    protected final ArtifactSearcher searcher = new ArtifactSearcherImpl();

    class UnrestrictedRootCreator extends UnrestrictedSessionRunner {

        protected DocumentRef rootRef;

        public DocumentRef getRootRef() {
            return rootRef;
        }

        UnrestrictedRootCreator(CoreSession session) {
            super(session);
        }

        @Override
        public void run() throws ClientException {
            DocumentModel root = session.createDocumentModel(Root_PATH, Root_NAME, "Folder");
            root.setProperty("dublincore", "title", Root_NAME);
            root = session.createDocument(root);

            ACL acl = new ACLImpl();
            acl.add(new ACE(Write_Grp, "Write", true));
            acl.add(new ACE(Read_Grp, "Read", true));
            ACP acp = root.getACP();
            acp.addACL(acl);
            session.setACP(root.getRef(), acp, true);

            rootRef = root.getRef();
            // flush caches
            session.save();
        }

    }

    protected DocumentModel getDocumentationRoot(CoreSession session) throws ClientException {

        DocumentRef rootRef = new PathRef(Root_PATH + Root_NAME);

        if (session.exists(rootRef)) {
            return session.getDocument(rootRef);
        }

        UnrestrictedRootCreator creator = new UnrestrictedRootCreator(session);

        creator.runUnrestricted();

        // flush caches
        session.save();
        return session.getDocument(creator.getRootRef());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter.isAssignableFrom(DocumentationService.class)) {
            return (T) this;
        } else if (adapter.isAssignableFrom(ArtifactSearcher.class)) {
            return (T) searcher;
        }
        return null;
    }

    @Override
    public Map<String, List<DocumentationItem>> listDocumentationItems(CoreSession session, String category,
            String targetType) {

        String query = "SELECT * FROM " + DocumentationItem.TYPE_NAME + " WHERE " + QueryHelper.NOT_DELETED;

        if (category != null) {
            query += " AND " + DocumentationItem.PROP_TYPE + " = " + NXQL.escapeString(category);
        }
        if (targetType != null) {
            query += " AND " + DocumentationItem.PROP_TARGET_TYPE + " = " + NXQL.escapeString(targetType);
        }

        query += " ORDER BY " + DocumentationItem.PROP_DOCUMENTATION_ID + ", dc:modified";
        List<DocumentModel> docs = session.query(query);

        Map<String, List<DocumentationItem>> sortMap = new HashMap<String, List<DocumentationItem>>();
        for (DocumentModel doc : docs) {
            DocumentationItem item = doc.getAdapter(DocumentationItem.class);

            List<DocumentationItem> alternatives = sortMap.get(item.getId());
            if (alternatives == null) {
                alternatives = new ArrayList<DocumentationItem>();
                alternatives.add(item);
                sortMap.put(item.getId(), alternatives);
            } else {
                alternatives.add(item);
            }
        }

        List<DocumentationItem> result = new ArrayList<DocumentationItem>();

        for (String documentationId : sortMap.keySet()) {
            DocumentationItem bestDoc = sortMap.get(documentationId).get(0);
            result.add(bestDoc);
        }

        Map<String, List<DocumentationItem>> sortedResult = new HashMap<String, List<DocumentationItem>>();
        Map<String, String> categories = getCategories();

        for (DocumentationItem item : result) {
            String key = item.getType();
            String label = categories.get(key);

            if (sortedResult.containsKey(label)) {
                sortedResult.get(label).add(item);
            } else {
                List<DocumentationItem> items = new ArrayList<DocumentationItem>();
                items.add(item);
                sortedResult.put(label, items);
            }
        }

        return sortedResult;
    }

    @Override
    public List<DocumentationItem> findDocumentItems(CoreSession session, NuxeoArtifact nxItem) throws ClientException {

        String id = nxItem.getId();
        String type = nxItem.getArtifactType();
        String query = "SELECT * FROM " + DocumentationItem.TYPE_NAME + " WHERE " + DocumentationItem.PROP_TARGET
                + " = " + NXQL.escapeString(id) + " AND " + DocumentationItem.PROP_TARGET_TYPE + " = "
                + NXQL.escapeString(type) + " AND " + QueryHelper.NOT_DELETED + " ORDER BY "
                + DocumentationItem.PROP_DOCUMENTATION_ID + ", dc:modified";
        List<DocumentModel> docs = session.query(query);

        Map<String, List<DocumentationItem>> sortMap = new HashMap<String, List<DocumentationItem>>();
        for (DocumentModel doc : docs) {
            DocumentationItem item = doc.getAdapter(DocumentationItem.class);

            List<DocumentationItem> alternatives = sortMap.get(item.getId());
            if (alternatives == null) {
                alternatives = new ArrayList<DocumentationItem>();
                alternatives.add(item);
                sortMap.put(item.getId(), alternatives);
            } else {
                alternatives.add(item);
            }
        }

        List<DocumentationItem> result = new ArrayList<DocumentationItem>();

        for (String documentationId : sortMap.keySet()) {
            DocumentationItem bestDoc = findBestMatch(nxItem, sortMap.get(documentationId));
            result.add(bestDoc);
        }
        return result;
    }

    protected DocumentationItem findBestMatch(NuxeoArtifact nxItem, List<DocumentationItem> docItems) {
        for (DocumentationItem docItem : docItems) {
            // get first possible because already sorted on modification date
            if (docItem.getApplicableVersion().contains(nxItem.getVersion())) {
                return docItem;
            }
        }
        // XXX may be find the closest match ?
        return docItems.get(0);
    }

    @Override
    public List<DocumentationItem> findDocumentationItemVariants(CoreSession session, DocumentationItem item)
            throws ClientException {

        List<DocumentationItem> result = new ArrayList<DocumentationItem>();
        List<DocumentModel> docs = findDocumentModelVariants(session, item);

        for (DocumentModel doc : docs) {
            DocumentationItem docItem = doc.getAdapter(DocumentationItem.class);
            if (docItem != null) {
                result.add(docItem);
            }
        }

        Collections.sort(result);
        Collections.reverse(result);

        return result;
    }

    public List<DocumentModel> findDocumentModelVariants(CoreSession session, DocumentationItem item)
            throws ClientException {
        String id = item.getId();
        String type = item.getTargetType();
        String query = "SELECT * FROM " + DocumentationItem.TYPE_NAME + " WHERE "
                + DocumentationItem.PROP_DOCUMENTATION_ID + " = " + NXQL.escapeString(id) + " AND "
                + DocumentationItem.PROP_TARGET_TYPE + " = " + NXQL.escapeString(type) + " AND "
                + QueryHelper.NOT_DELETED;
        query += " ORDER BY dc:created";
        return session.query(query);
    }

    @Override
    public DocumentationItem createDocumentationItem(CoreSession session, NuxeoArtifact item, String title,
            String content, String type, List<String> applicableVersions, boolean approved, String renderingType)
            throws ClientException {

        DocumentModel doc = session.createDocumentModel(DocumentationItem.TYPE_NAME);

        String name = title + '-' + item.getId();
        name = IdUtils.generateId(name, "-", true, 64);

        UUID docUUID = UUID.nameUUIDFromBytes(name.getBytes());

        doc.setPathInfo(getDocumentationRoot(session).getPathAsString(), name);
        doc.setPropertyValue("dc:title", title);
        Blob blob = Blobs.createBlob(content);
        blob.setFilename(type);
        doc.setPropertyValue("file:content", (Serializable) blob);
        doc.setPropertyValue(DocumentationItem.PROP_TARGET, item.getId());
        doc.setPropertyValue(DocumentationItem.PROP_TARGET_TYPE, item.getArtifactType());
        doc.setPropertyValue(DocumentationItem.PROP_DOCUMENTATION_ID, docUUID.toString());
        doc.setPropertyValue(DocumentationItem.PROP_NUXEO_APPROVED, Boolean.valueOf(approved));
        doc.setPropertyValue(DocumentationItem.PROP_TYPE, type);
        doc.setPropertyValue(DocumentationItem.PROP_RENDERING_TYPE, renderingType);
        doc.setPropertyValue(DocumentationItem.PROP_APPLICABLE_VERSIONS, (Serializable) applicableVersions);

        doc = session.createDocument(doc);
        session.save();

        return doc.getAdapter(DocumentationItem.class);
    }

    @Override
    public void deleteDocumentationItem(CoreSession session, String uuid) throws ClientException {
        DocumentModel doc = session.getDocument(new IdRef(uuid));
        // check type
        if (!doc.getType().equals(DocumentationItem.TYPE_NAME)) {
            throw new RuntimeException("Invalid documentation item");
        }
        // check under our root
        DocumentModel root = getDocumentationRoot(session);
        DocumentModel parent = session.getDocument(doc.getParentRef());
        if (!root.getId().equals(parent.getId())) {
            throw new RuntimeException("Invalid documentation item");
        }
        // ok to delete
        session.removeDocument(doc.getRef());
    }

    protected DocumentModel updateDocumentModel(DocumentModel doc, DocumentationItem item) throws ClientException {

        doc.setPropertyValue("dc:title", item.getTitle());
        Blob content = Blobs.createBlob(item.getContent());
        content.setFilename(item.getTypeLabel());
        doc.setPropertyValue("file:content", (Serializable) content);
        doc.setPropertyValue(DocumentationItem.PROP_DOCUMENTATION_ID, item.getId());
        doc.setPropertyValue(DocumentationItem.PROP_NUXEO_APPROVED, Boolean.valueOf(item.isApproved()));
        doc.setPropertyValue(DocumentationItem.PROP_RENDERING_TYPE, item.getRenderingType());
        doc.setPropertyValue(DocumentationItem.PROP_APPLICABLE_VERSIONS, (Serializable) item.getApplicableVersion());

        List<Map<String, Serializable>> atts = new ArrayList<Map<String, Serializable>>();
        Map<String, String> attData = item.getAttachments();
        if (attData != null && attData.size() > 0) {
            for (String fileName : attData.keySet()) {
                Map<String, Serializable> fileItem = new HashMap<String, Serializable>();
                Blob blob = Blobs.createBlob(attData.get(fileName));
                blob.setFilename(fileName);

                fileItem.put("file", (Serializable) blob);
                fileItem.put("filename", fileName);

                atts.add(fileItem);
            }
            doc.setPropertyValue("files:files", (Serializable) atts);
        }

        return doc;
    }

    @Override
    public DocumentationItem updateDocumentationItem(CoreSession session, DocumentationItem docItem)
            throws ClientException {

        DocumentModel existingDoc = session.getDocument(new IdRef(docItem.getUUID()));
        DocumentationItem existingDocItem = existingDoc.getAdapter(DocumentationItem.class);

        List<String> applicableVersions = docItem.getApplicableVersion();
        List<String> existingApplicableVersions = existingDocItem.getApplicableVersion();
        List<String> discardedVersion = new ArrayList<String>();

        for (String version : existingApplicableVersions) {
            if (!applicableVersions.contains(version)) {
                discardedVersion.add(version);
            }
            // XXX check for older versions in case of inconsistent
            // applicableVersions values ...
        }

        if (discardedVersion.size() > 0) {
            // save old version
            String newName = existingDoc.getName();
            Collections.sort(discardedVersion);
            for (String version : discardedVersion) {
                newName = newName + "_" + version;
            }
            newName = IdUtils.generateId(newName, "-", true, 100);

            DocumentModel discardedDoc = session.copy(existingDoc.getRef(), existingDoc.getParentRef(), newName);
            discardedDoc.setPropertyValue(DocumentationItem.PROP_APPLICABLE_VERSIONS, (Serializable) discardedVersion);

            discardedDoc = session.saveDocument(discardedDoc);
        }

        existingDoc = updateDocumentModel(existingDoc, docItem);
        existingDoc = session.saveDocument(existingDoc);
        session.save();
        return existingDoc.getAdapter(DocumentationItem.class);
    }

    protected List<DocumentModel> listCategories() {
        DirectoryService dm = Framework.getService(DirectoryService.class);
        Session session = dm.open(DIRECTORY_NAME);
        try {
            return session.query(Collections.<String, Serializable> emptyMap(), null,
                    Collections.singletonMap("ordering", "ASC"));
        } finally {
            session.close();
        }
    }

    @Override
    public List<String> getCategoryKeys() {
        List<String> categories = new ArrayList<String>();
        for (DocumentModel entry : listCategories()) {
            categories.add(entry.getId());
        }
        return categories;
    }

    @Override
    public Map<String, String> getCategories() {
        Map<String, String> categories = new LinkedHashMap<String, String>();
        if (!Framework.isTestModeSet()) {
            for (DocumentModel entry : listCategories()) {
                String value = (String) entry.getProperty("vocabulary", "label");
                categories.put(entry.getId(), value);
            }
        } else {
            categories.put("description", "Description");
            categories.put("codeSample", "Code Sample");
            categories.put("howTo", "How To");
        }
        return categories;
    }

    @Override
    public void exportDocumentation(CoreSession session, OutputStream out) {
        try {
            String query = "SELECT * FROM " + DocumentationItem.TYPE_NAME + " WHERE " + QueryHelper.NOT_DELETED;
            DocumentModelList docList = session.query(query);
            DocumentReader reader = new DocumentModelListReader(docList);
            DocumentWriter writer = new NuxeoArchiveWriter(out);
            DocumentPipe pipe = new DocumentPipeImpl(10);
            pipe.setReader(reader);
            pipe.setWriter(writer);
            pipe.run();
            reader.close();
            writer.close();
        } catch (IOException | ClientException e) {
            log.error("Error while exporting documentation", e);
        }
    }

    @Override
    public void importDocumentation(CoreSession session, InputStream is) {
        try {
            String importPath = getDocumentationRoot(session).getPathAsString();
            DocumentReader reader = new NuxeoArchiveReader(is);
            DocumentWriter writer = new DocumentModelWriter(session, importPath);

            DocumentPipe pipe = new DocumentPipeImpl(10);
            pipe.setReader(reader);
            pipe.setWriter(writer);
            DocumentTransformer rootCutter = new DocumentTransformer() {
                @Override
                public boolean transform(ExportedDocument doc) {
                    doc.setPath(doc.getPath().removeFirstSegments(1));
                    return true;
                }
            };
            pipe.addTransformer(rootCutter);
            pipe.run();
            reader.close();
            writer.close();
        } catch (IOException | ClientException e) {
            log.error("Error while importing documentation", e);
        }
    }

    @Override
    public String getDocumentationStats(CoreSession session) {
        String result = "";
        try {
            String query = "SELECT * FROM " + DocumentationItem.TYPE_NAME + " WHERE " + QueryHelper.NOT_DELETED;
            DocumentModelList docList = session.query(query);
            result = docList.size() + " documents";

        } catch (ClientException e) {
            log.error("Error while exporting documentation", e);
        }
        return result;
    }

    @Override
    public Map<String, DocumentationItem> getAvailableDescriptions(CoreSession session, String targetType) {

        Map<String, List<DocumentationItem>> itemsByCat = listDocumentationItems(session,
                DefaultDocumentationType.DESCRIPTION.getValue(), targetType);
        Map<String, DocumentationItem> result = new HashMap<String, DocumentationItem>();

        if (itemsByCat.size() > 0) {
            String labelKey = itemsByCat.keySet().iterator().next();
            List<DocumentationItem> docs = itemsByCat.get(labelKey);
            for (DocumentationItem doc : docs) {
                result.put(doc.getTarget(), doc);
            }
        }

        return result;
    }

}
