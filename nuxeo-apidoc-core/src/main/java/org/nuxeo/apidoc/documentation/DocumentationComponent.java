package org.nuxeo.apidoc.documentation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.api.DocumentationItem;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

public class DocumentationComponent extends DefaultComponent implements
        DocumentationService {

    public static final String DIRECTORY_NAME = "documentationTypes";

    protected DocumentModel getDocumentationRoot(CoreSession session)
            throws ClientException {
        return session.getRootDocument();
    }

    public List<DocumentationItem> findDocumentItems(CoreSession session,
            NuxeoArtifact nxItem) throws ClientException {

        String id = nxItem.getId();
        String type = nxItem.getArtifactType();
        String query = "select * from NXDocumentation where nxdoc:target='" + id + "' AND nxdoc:targetType='" + type + "' AND ecm:currentLifeCycleState != 'deleted' ORDER BY nxdoc:documentationId, dc:modified";
        List<DocumentModel> docs =  session.query(query);

        Map<String, List<DocumentationItem>> sortMap = new HashMap<String, List<DocumentationItem>>();
        for (DocumentModel doc : docs) {
            DocumentationItem item = doc.getAdapter(DocumentationItem.class);

            List<DocumentationItem> alternatives = sortMap.get(item.getId());
            if (alternatives==null) {
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

    public List<DocumentationItem> findDocumentationItemVariants(
            CoreSession session, DocumentationItem item) throws ClientException {

        List<DocumentationItem> result = new ArrayList<DocumentationItem>();
        List<DocumentModel> docs = findDocumentModelVariants(session, item);

        for (DocumentModel doc: docs) {
            DocumentationItem docItem = doc.getAdapter(DocumentationItem.class);
            if (docItem!=null) {
                result.add(docItem);
            }
        }
        return result;
    }

    public List<DocumentModel> findDocumentModelVariants(
            CoreSession session, DocumentationItem item) throws ClientException {

        String id = item.getId();
        String type = item.getTargetType();
        String query = "select * from NXDocumentation where nxdoc:documentationId='" + id + "' AND nxdoc:targetType='" + type + "'AND ecm:currentLifeCycleState != 'deleted'";
        return session.query(query);

    }

    public DocumentationItem createDocumentationItem(CoreSession session,
            NuxeoArtifact item, String title, String content, String type,
            List<String> applicableVersions, boolean approved,
            String renderingType) throws ClientException {

        DocumentModel doc = session
                .createDocumentModel(DocumentationItemDocAdapter.DOC_TYPE);

        String name = title + '-' + item.getId();
        name = IdUtils.generateId(name, "-", true, 50);

        doc.setPathInfo(getDocumentationRoot(session).getPathAsString(), name);
        doc.setPropertyValue("dc:title", title);
        Blob blob = new StringBlob(content);
        doc.setPropertyValue("file:content", (Serializable)blob);
        doc.setPropertyValue("nxdoc:target", item.getId());
        doc.setPropertyValue("nxdoc:targetType", item.getArtifactType());
        doc.setPropertyValue("nxdoc:documentationId", name);
        doc.setPropertyValue("nxdoc:nuxeoApproved", approved);
        doc.setPropertyValue("nxdoc:type", type);
        doc.setPropertyValue("nxdoc:renderingType", renderingType);
        doc.setPropertyValue("nxdoc:applicableVersions",
                (Serializable) applicableVersions);

        doc = session.createDocument(doc);
        session.save();

        return doc.getAdapter(DocumentationItem.class);
    }

    protected DocumentModel updateDocumenModel(DocumentModel doc, DocumentationItem item) throws ClientException{

        doc.setPropertyValue("dc:title", item.getTitle());
        doc.setPropertyValue("file:content", new StringBlob(item.getContent()));
        doc.setPropertyValue("nxdoc:documentationId", item.getId());
        doc.setPropertyValue("nxdoc:nuxeoApproved", item.isApproved());
        doc.setPropertyValue("nxdoc:renderingType", item.getRenderingType());
        doc.setPropertyValue("nxdoc:applicableVersions",
                (Serializable) item.getApplicableVersion());


        return doc;
    }

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
            // XXX check for older versions in case of inconsistent applicableVersions values ...
        }

        if (discardedVersion.size()>0) {
            // save old version
            String newName = existingDoc.getName();
            Collections.sort(discardedVersion);
            for (String version : discardedVersion) {
                newName = newName + "_" + version;
            }
            newName = IdUtils.generateId(newName, "-", true, 100);

            DocumentModel discardedDoc = session.copy(existingDoc.getRef(), existingDoc.getParentRef(), newName);
            discardedDoc.setPropertyValue("nxdoc:applicableVersions", (Serializable) discardedVersion);

            discardedDoc = session.saveDocument(discardedDoc);
        }

        existingDoc = updateDocumenModel(existingDoc, docItem);
        existingDoc = session.saveDocument(existingDoc);
        session.save();
        return existingDoc.getAdapter(DocumentationItem.class);

      }


    public List<String> getCategoryKeys()  throws Exception {

        List<String> categories = new ArrayList<String>();

        DirectoryService dm = Framework.getService(DirectoryService.class);
        Session session = dm.open(DIRECTORY_NAME);
        try {
            DocumentModelList entries = session.getEntries();
            for (DocumentModel entry : entries) {
                categories.add(entry.getId());
            }
        } finally {
            session.close();
        }
        return categories;
    }

    public Map<String, String> getCategories()  throws Exception {

        Map<String, String> categories = new HashMap<String, String>();

        DirectoryService dm = Framework.getService(DirectoryService.class);
        Session session = dm.open(DIRECTORY_NAME);
        try {
            DocumentModelList entries = session.getEntries();
            for (DocumentModel entry : entries) {

                String value = (String) entry.getProperty("vocabulary", "label");
                categories.put(entry.getId(), value);
            }
        } finally {
            session.close();
        }
        return categories;
    }


}
