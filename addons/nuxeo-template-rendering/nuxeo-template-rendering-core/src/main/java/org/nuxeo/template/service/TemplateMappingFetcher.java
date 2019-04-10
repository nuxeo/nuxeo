package org.nuxeo.template.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.template.adapters.source.TemplateSourceDocumentAdapterImpl;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

public class TemplateMappingFetcher extends UnrestrictedSessionRunner {

    protected static String repoName;

    protected static final Log log = LogFactory.getLog(TemplateMappingFetcher.class);

    protected static String getRepoName() {
        if (repoName == null) {
            RepositoryManager rm = Framework.getLocalService(RepositoryManager.class);
            repoName = rm.getDefaultRepositoryName();
        }
        return repoName;
    }

    protected Map<String, List<String>> mapping = new HashMap<String, List<String>>();

    protected TemplateMappingFetcher() {
        super(getRepoName());
    }

    @Override
    public void run() throws ClientException {
        StringBuffer sb = new StringBuffer("select * from Document where ");
        sb.append(TemplateSourceDocumentAdapterImpl.TEMPLATE_FORCED_TYPES_ITEM_PROP);
        sb.append(" <> 'none'");

        DocumentModelList docs = session.query(sb.toString());

        for (DocumentModel doc : docs) {
            TemplateSourceDocument tmpl = doc.getAdapter(TemplateSourceDocument.class);
            if (tmpl != null) {
                for (String type : tmpl.getForcedTypes()) {
                    if (mapping.containsKey(type)) {
                        mapping.get(type).add(doc.getId());
                    } else {
                        List<String> templates = new ArrayList<String>();
                        templates.add(doc.getId());
                        mapping.put(type, templates);
                    }
                }
            }
        }
    }

    public Map<String, List<String>> getMapping() {
        return mapping;
    }
}
