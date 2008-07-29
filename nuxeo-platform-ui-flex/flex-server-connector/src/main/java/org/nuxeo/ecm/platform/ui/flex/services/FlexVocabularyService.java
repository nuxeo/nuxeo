package org.nuxeo.ecm.platform.ui.flex.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.remoting.WebRemote;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.runtime.api.Framework;

@Name("flexVocabularyService")
@Scope(ScopeType.STATELESS)
public class FlexVocabularyService {

    private java.util.Locale translationLocal = java.util.Locale.getDefault();

    private String getTranslation(String key, java.util.Locale local) {
        try {
            ResourceBundle bundle = ResourceBundle.getBundle("messages", local,
                    Thread.currentThread().getContextClassLoader());
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }

    @WebRemote
    public List<Object> getVocabularyEntries(String vocName) throws Exception {
        return getVocabularyEntries(vocName, null);
    }

    @WebRemote
    public List<Object> getVocabularyEntries(String vocName, String parentKey)
            throws Exception {
        DirectoryService directoryService = Framework.getService(DirectoryService.class);
        Session dirSession = directoryService.open(vocName);

        String directorySchema = directoryService.getDirectorySchema(vocName);

        List<Object> entries = new ArrayList<Object>();

        if (directorySchema.equals("vocabulary")) {
            for (DocumentModel entry : dirSession.getEntries()) {

                Map<String, String> mapEntry = new HashMap<String, String>();

                String label = getTranslation((String) entry.getProperty(
                        "vocabulary", "label"), translationLocal);
                mapEntry.put("label", label);
                mapEntry.put("data", entry.getId());
                entries.add(mapEntry);

            }
        } else if (directorySchema.equals("xvocabulary")) {

            Map<String, Object> filter = new HashMap<String, Object>();

            if (parentKey != null)
                filter.put("parent", parentKey);

            for (DocumentModel entry : dirSession.query(filter)) {

                Map<String, String> mapEntry = new HashMap<String, String>();

                String label = getTranslation((String) entry.getProperty(
                        "xvocabulary", "label"), translationLocal);
                mapEntry.put("label", label);
                mapEntry.put("data", entry.getId());
                entries.add(mapEntry);

            }
        }
        try {
            dirSession.close();
        } catch (ClientException e) {
            // XXX
        }
        return entries;
    }
}
