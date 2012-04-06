package org.nuxeo.template.fm;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryManager;
import org.nuxeo.ecm.platform.rendering.fm.adapters.DocumentObjectWrapper;
import org.nuxeo.runtime.api.Framework;

import freemarker.template.TemplateModel;

public class ContextFunctions {

    protected final DocumentModel doc;

    protected final DocumentObjectWrapper nuxeoWrapper;

    public ContextFunctions(DocumentModel doc,
            DocumentObjectWrapper nuxeoWrapper) {
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

    public List<TemplateModel> getChildren() throws Exception {
        List<DocumentModel> children = doc.getCoreSession().getChildren(
                doc.getRef());
        List<TemplateModel> docs = new ArrayList<TemplateModel>();
        for (DocumentModel child : children) {
            docs.add(nuxeoWrapper.wrap(child));
        }
        return docs;
    }

}
