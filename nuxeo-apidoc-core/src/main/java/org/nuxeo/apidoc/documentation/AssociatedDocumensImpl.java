package org.nuxeo.apidoc.documentation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.apidoc.api.AssociatedDocuments;
import org.nuxeo.apidoc.api.DocumentationItem;
import org.nuxeo.apidoc.api.NuxeoArtifact;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

public class AssociatedDocumensImpl implements AssociatedDocuments {

    public static final String DIRECTORY_NAME = "documentationTypes";

    protected String id;

    protected CoreSession session;

    public AssociatedDocumensImpl(NuxeoArtifact item, CoreSession session) {
        id = item.getId();
        this.session = session;
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

    public DocumentationItem createDocumentationItem(NuxeoArtifact item, String title,String content, String type, List<String> applicableVersions, boolean approved, String renderingType) throws ClientException {
        return Framework.getLocalService(DocumentationService.class).createDocumentationItem(session, item, title, content, type, applicableVersions, approved, renderingType);
    }

    public DocumentationItem updateDocumentationItem(String title,String content, List<String> applicableVersions, boolean approved, String renderingType) {
        return null;
    }

    public List<DocumentationItem> getDocumentationInCategory(String categoryKey) {
        return null;
    }


}
