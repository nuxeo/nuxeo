package org.nuxeo.ecm.platform.template.service;

import java.util.List;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.template.adapters.source.TemplateSourceDocument;
import org.nuxeo.ecm.platform.template.processors.TemplateProcessor;

public interface TemplateProcessorService {

    String findProcessorName(Blob templateBlob);

    TemplateProcessor findProcessor(Blob templateBlob);

    TemplateProcessor getProcessor(String name);

    List<DocumentModel> getAvailableTemplateDocs(CoreSession session,
            String targetType) throws ClientException;

    List<TemplateSourceDocument> getAvailableTemplates(CoreSession session,
            String targetType) throws ClientException;

}
