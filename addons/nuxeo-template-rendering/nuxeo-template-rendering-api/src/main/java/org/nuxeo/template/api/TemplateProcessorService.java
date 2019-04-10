package org.nuxeo.template.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;
import org.nuxeo.template.api.context.DocumentWrapper;
import org.nuxeo.template.api.descriptor.ContextExtensionFactoryDescriptor;
import org.nuxeo.template.api.descriptor.TemplateProcessorDescriptor;

/**
 * This is the service interface to manage {@link TemplateProcessor} and
 * associated templates.
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * 
 */
public interface TemplateProcessorService {

    /**
     * Finds the template processor name for a given {@link Blob}. The template
     * processor is found based on mime-types.
     * 
     * @param templateBlob
     * @return the {@link TemplateProcessor} name
     */
    String findProcessorName(Blob templateBlob);

    /**
     * Finds the {@link TemplateProcessor} for a given {@link Blob}. The
     * template processor is found based on mime-types.
     * 
     * @param templateBlob
     * @return the {@link TemplateProcessor}
     */
    TemplateProcessor findProcessor(Blob templateBlob);

    /**
     * Get a {@link TemplateProcessor} by it's name. Name is defined in the
     * associated descriptor.
     * 
     * @param name
     * @return the {@link TemplateProcessor}
     */
    TemplateProcessor getProcessor(String name);

    /**
     * Returns all registered {@link TemplateProcessor}s
     * 
     * @return collection of registered {@link TemplateProcessorDescriptor}
     */
    Collection<TemplateProcessorDescriptor> getRegisteredTemplateProcessors();

    /**
     * Find {@link TemplateSourceDocument}s that can be bound to a given doc
     * type.
     * 
     * @param session
     * @param targetType the target Document Type
     * @return List of applicable DocumentModel
     * @throws ClientException
     */
    List<DocumentModel> getAvailableTemplateDocs(CoreSession session,
            String targetType) throws ClientException;

    /**
     * Find {@link TemplateSourceDocument}s that can be bound to a given doc
     * type.
     * 
     * @param session
     * @param targetType the target Document Type
     * @return List of applicable {@link TemplateSourceDocument}
     * @throws ClientException
     */
    List<TemplateSourceDocument> getAvailableTemplates(CoreSession session,
            String targetType) throws ClientException;

    /**
     * Retrieve the {@link TemplateSourceDocument} that can be used as an Office
     * template (i.e that support to store the template file as main blob of
     * target DocumentModel)
     * 
     * @param session
     * @param targetType
     * @return
     * @throws ClientException
     */
    List<TemplateSourceDocument> getAvailableOfficeTemplates(
            CoreSession session, String targetType) throws ClientException;

    /**
     * Retrieve the DocumentModels using a given {@link TemplateSourceDocument}
     * 
     * @param source the {@link TemplateSourceDocument}
     * @return
     * @throws ClientException
     */
    List<TemplateBasedDocument> getLinkedTemplateBasedDocuments(
            DocumentModel source) throws ClientException;

    /**
     * Retrieve the Map used for mapping Document Types to Template Name. This
     * Map represent the Template that must be automatically bound at creation
     * time for each Document Type.
     * 
     * @return the Type2Template mapping
     */
    Map<String, String> getTypeMapping();

    /**
     * Update Type2Template Mapping from the data contained in the source
     * DocumentModel.
     * 
     * @param doc
     * @throws ClientException
     */
    void registerTypeMapping(DocumentModel doc) throws ClientException;

    /**
     * Associate a {@link DocumentModel} to a {@link TemplateSourceDocument}. If
     * the DocumentModel is not already a {@link TemplateBasedDocument}, the
     * associated facet will be automatically added.
     * 
     * @param targetDoc the DocumentModel to associate to a template
     * @param sourceTemplateDoc the DocumentModel holding the template
     * @param save flag to indicate if target DocumentModel must be saved or not
     * @return the updated DocumentModel
     * @throws ClientException
     */
    DocumentModel makeTemplateBasedDocument(DocumentModel targetDoc,
            DocumentModel sourceTemplateDoc, boolean save)
            throws ClientException;

    /**
     * Detach a Template from a {@link DocumentModel}
     * 
     * @param targetDoc the DocumentModel to detach
     * @param templateName the name of the template to detach
     * @param save save flag to indicate if target DocumentModel must be saved
     *            or not
     * @return the updated DocumentModel
     * @throws ClientException
     */
    DocumentModel detachTemplateBasedDocument(DocumentModel targetDoc,
            String templateName, boolean save) throws ClientException;

    void addContextExtensions(DocumentModel currentDocument,
            DocumentWrapper wrapper, Map<String, Object> ctx);

    Map<String, ContextExtensionFactoryDescriptor> getRegistredContextExtensions();

    List<String> getReservedContextKeywords();
}
