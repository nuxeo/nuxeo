package org.nuxeo.ecm.platform.template.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.template.adapters.doc.TemplateBasedDocument;
import org.nuxeo.ecm.platform.template.adapters.source.TemplateSourceDocument;
import org.nuxeo.ecm.platform.template.processors.TemplateProcessor;

public interface TemplateProcessorService {

    String findProcessorName(Blob templateBlob);

    TemplateProcessor findProcessor(Blob templateBlob);

    TemplateProcessor getProcessor(String name);

    Collection<TemplateProcessorDescriptor> getRegistredTemplateProcessors();

    List<DocumentModel> getAvailableTemplateDocs(CoreSession session,
            String targetType) throws ClientException;

    List<TemplateSourceDocument> getAvailableTemplates(CoreSession session,
            String targetType) throws ClientException;

    List<TemplateBasedDocument> getLinkedTemplateBasedDocuments(DocumentModel source) throws ClientException;

    Map<String, String> getTypeMapping();

    void registerTypeMapping(DocumentModel doc) throws ClientException;

    DocumentModel makeTemplateBasedDocument(DocumentModel targetDoc, DocumentModel sourceTemplateDoc, boolean save) throws ClientException;
}
