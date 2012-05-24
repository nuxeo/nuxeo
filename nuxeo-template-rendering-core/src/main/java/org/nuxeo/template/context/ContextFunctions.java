package org.nuxeo.template.context;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryManager;
import org.nuxeo.runtime.api.Framework;

public class ContextFunctions {

    protected final DocumentModel doc;

    protected final DocumentWrapper nuxeoWrapper;

    public ContextFunctions(DocumentModel doc, DocumentWrapper nuxeoWrapper) {
        this.doc = doc;
        this.nuxeoWrapper = nuxeoWrapper;
    }

    public String getVocabularyLabel(String voc_name, String key)
            throws Exception {

        DirectoryManager dm = Framework.getLocalService(DirectoryManager.class);

        if (dm.getDirectoryNames().contains(voc_name)) {
            Directory dir = dm.getDirectory(voc_name);

            String schema = dir.getSchema();
            if ("vocabulary".equals(schema) || "xvocabulary".equals(schema)) {
                Session session = dir.getSession();
                DocumentModel entry = session.getEntry(key);
                if (entry != null) {
                    return (String) entry.getProperty(schema, "label");
                }
            }
        }
        return key;
    }

    public List<Object> getChildren() throws Exception {
        List<DocumentModel> children = doc.getCoreSession().getChildren(
                doc.getRef());
        List<Object> docs = new ArrayList<Object>();
        for (DocumentModel child : children) {
            docs.add(nuxeoWrapper.wrap(child));
        }
        return docs;
    }

}
