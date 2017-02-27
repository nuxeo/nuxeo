/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Thierry Delprat
 */
package org.nuxeo.template.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.template.api.adapters.TemplateBasedDocument;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;
import org.nuxeo.template.api.context.DocumentWrapper;
import org.nuxeo.template.api.descriptor.ContextExtensionFactoryDescriptor;
import org.nuxeo.template.api.descriptor.OutputFormatDescriptor;
import org.nuxeo.template.api.descriptor.TemplateProcessorDescriptor;

/**
 * This is the service interface to manage {@link TemplateProcessor} and associated templates.
 *
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 */
public interface TemplateProcessorService {

    /**
     * Finds the template processor name for a given {@link Blob}. The template processor is found based on mime-types.
     *
     * @param templateBlob
     * @return the {@link TemplateProcessor} name
     */
    String findProcessorName(Blob templateBlob);

    /**
     * Finds the {@link TemplateProcessor} for a given {@link Blob}. The template processor is found based on
     * mime-types.
     *
     * @param templateBlob
     * @return the {@link TemplateProcessor}
     */
    TemplateProcessor findProcessor(Blob templateBlob);

    /**
     * Get a {@link TemplateProcessor} by it's name. Name is defined in the associated descriptor.
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
     * Find {@link TemplateSourceDocument}s that can be bound to a given doc type.
     *
     * @param session
     * @param targetType the target Document Type
     * @return List of applicable DocumentModel
     */
    List<DocumentModel> getAvailableTemplateDocs(CoreSession session, String targetType);

    /**
     * Find {@link TemplateSourceDocument}s that can be bound to a given doc type.
     *
     * @param session
     * @param targetType the target Document Type
     * @return List of applicable {@link TemplateSourceDocument}
     */
    List<TemplateSourceDocument> getAvailableTemplates(CoreSession session, String targetType);

    /**
     * Returns a template with a given templateName.
     *
     * @param session
     * @param name the name of the template
     * @return
     *
     * @since 9.1
     */
    DocumentModel getTemplateDoc(CoreSession session, String name);

    /**
     * Retrieve the {@link TemplateSourceDocument} that can be used as an Office template (i.e that support to store the
     * template file as main blob of target DocumentModel)
     *
     * @param session
     * @param targetType
     * @return
     */
    List<TemplateSourceDocument> getAvailableOfficeTemplates(CoreSession session, String targetType);

    /**
     * Retrieve the DocumentModels using a given {@link TemplateSourceDocument}
     *
     * @param source the {@link TemplateSourceDocument}
     * @return
     */
    List<TemplateBasedDocument> getLinkedTemplateBasedDocuments(DocumentModel source);

    /**
     * Retrieve the Map used for mapping Document Types to Template Names. This Map represent the Templates that must be
     * automatically bound at creation time for each Document Type.
     *
     * @return the Type2Template mapping
     */
    Map<String, List<String>> getTypeMapping();

    /**
     * Update Type2Template Mapping from the data contained in the source DocumentModel.
     *
     * @param doc
     */
    void registerTypeMapping(DocumentModel doc);

    /**
     * Associate a {@link DocumentModel} to a {@link TemplateSourceDocument}. If the DocumentModel is not already a
     * {@link TemplateBasedDocument}, the associated facet will be automatically added.
     *
     * @param targetDoc the DocumentModel to associate to a template
     * @param sourceTemplateDoc the DocumentModel holding the template
     * @param save flag to indicate if target DocumentModel must be saved or not
     * @return the updated DocumentModel
     */
    DocumentModel makeTemplateBasedDocument(DocumentModel targetDoc, DocumentModel sourceTemplateDoc, boolean save);

    /**
     * Detach a Template from a {@link DocumentModel}
     *
     * @param targetDoc the DocumentModel to detach
     * @param templateName the name of the template to detach
     * @param save save flag to indicate if target DocumentModel must be saved or not
     * @return the updated DocumentModel
     */
    DocumentModel detachTemplateBasedDocument(DocumentModel targetDoc, String templateName, boolean save);

    void addContextExtensions(DocumentModel currentDocument, DocumentWrapper wrapper, Map<String, Object> ctx);

    Map<String, ContextExtensionFactoryDescriptor> getRegistredContextExtensions();

    List<String> getReservedContextKeywords();

    /**
     * @return the list of registered Ouput formats used to convert output of a rendered document.
     */
    Collection<OutputFormatDescriptor> getOutputFormats();

    /**
     * The returned {@link OutputFormatDescriptor} contains either an operation chain or a mime-type use to convert the
     * output of a rendered document.
     *
     * @param outputFormatId
     * @return {@link OutputFormatDescriptor}
     */
    OutputFormatDescriptor getOutputFormatDescriptor(String outputFormatId);

}
