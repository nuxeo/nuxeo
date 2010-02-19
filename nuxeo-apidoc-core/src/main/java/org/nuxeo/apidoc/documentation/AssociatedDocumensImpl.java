package org.nuxeo.apidoc.documentation;

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

    protected NuxeoArtifact item;

    protected CoreSession session;

    public AssociatedDocumensImpl(NuxeoArtifact item, CoreSession session) {
        this.item = item;
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

    public Map<String, List<DocumentationItem>> getDocumentationItems(CoreSession session) throws Exception {

        DocumentationService ds = Framework.getLocalService(DocumentationService.class);

        List<DocumentationItem> docItems = ds.findDocumentItems(session, item);

        Map<String, List<DocumentationItem>> result = new HashMap<String, List<DocumentationItem>>();

        Map<String, String> categories = getCategories();

        for (DocumentationItem docItem : docItems) {

            String cat = docItem.getType();
            String catLabel = categories.get(cat);

            List<DocumentationItem> itemList = result.get(catLabel);

            if (itemList!=null) {
                itemList.add(docItem);
            } else {
                itemList = new ArrayList<DocumentationItem>();
                itemList.add(docItem);
                result.put(catLabel, itemList);
            }
        }
        return result;
    }

}
