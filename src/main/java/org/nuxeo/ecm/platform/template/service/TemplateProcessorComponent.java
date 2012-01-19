package org.nuxeo.ecm.platform.template.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.template.adapters.doc.TemplateBasedDocument;
import org.nuxeo.ecm.platform.template.adapters.doc.TemplateBasedDocumentAdapterImpl;
import org.nuxeo.ecm.platform.template.adapters.source.TemplateSourceDocument;
import org.nuxeo.ecm.platform.template.processors.TemplateProcessor;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class TemplateProcessorComponent extends DefaultComponent implements
        TemplateProcessorService {

    protected static final Log log = LogFactory.getLog(TemplateProcessorComponent.class);

    public static final String PROCESSOR_XP = "processor";

    protected List<TemplateProcessorDescriptor> templateProcessors = new ArrayList<TemplateProcessorDescriptor>();

    protected TemplateProcessorRegistry processorRegistry;

    protected ConcurrentHashMap<String, String> type2Template = null;

    @Override
    public void activate(ComponentContext context) throws Exception {
        processorRegistry = new TemplateProcessorRegistry();
    }

    @Override
    public void deactivate(ComponentContext context) throws Exception {
        processorRegistry = null;
    }

    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (PROCESSOR_XP.equals(extensionPoint)) {
            processorRegistry.addContribution((TemplateProcessorDescriptor) contribution);
        }
    }

    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        if (PROCESSOR_XP.equals(extensionPoint)) {
            processorRegistry.removeContribution((TemplateProcessorDescriptor) contribution);
        }
    }

    @Override
    public TemplateProcessor findProcessor(Blob templateBlob) {
        TemplateProcessorDescriptor desc = findProcessorDescriptor(templateBlob);
        if (desc != null) {
            return desc.getProcessor();
        } else {
            return null;
        }
    }

    @Override
    public String findProcessorName(Blob templateBlob) {
        TemplateProcessorDescriptor desc = findProcessorDescriptor(templateBlob);
        if (desc != null) {
            return desc.getName();
        } else {
            return null;
        }
    }

    public TemplateProcessorDescriptor findProcessorDescriptor(Blob templateBlob) {
        TemplateProcessorDescriptor processor = null;
        String mt = templateBlob.getMimeType();
        if (mt != null) {
            processor = findProcessorByMimeType(mt);
        }
        if (processor == null) {
            String fileName = templateBlob.getFilename();
            if (fileName != null) {
                String ext = FileUtils.getFileExtension(fileName);
                processor = findProcessorByExtension(ext);
            }
        }
        return processor;
    }

    protected TemplateProcessorDescriptor findProcessorByMimeType(String mt) {

        List<TemplateProcessorDescriptor> candidates = new ArrayList<TemplateProcessorDescriptor>();
        for (TemplateProcessorDescriptor desc : processorRegistry.getRegistredProcessors()) {
            if (desc.getSupportedMimeTypes().contains(mt)) {
                if (desc.isDefaultProcessor()) {
                    return desc;
                } else {
                    candidates.add(desc);
                }
            }
        }
        if (candidates.size() > 0) {
            return candidates.get(0);
        }
        return null;
    }

    protected TemplateProcessorDescriptor findProcessorByExtension(
            String extension) {

        List<TemplateProcessorDescriptor> candidates = new ArrayList<TemplateProcessorDescriptor>();
        for (TemplateProcessorDescriptor desc : processorRegistry.getRegistredProcessors()) {
            if (desc.getSupportedExtensions().contains(extension)) {
                if (desc.isDefaultProcessor()) {
                    return desc;
                } else {
                    candidates.add(desc);
                }
            }
        }
        if (candidates.size() > 0) {
            return candidates.get(0);
        }
        return null;
    }

    public TemplateProcessorDescriptor getDescriptor(String name) {
        return processorRegistry.getProcessorByName(name);
    }

    @Override
    public TemplateProcessor getProcessor(String name) {
        if (name == null) {
            log.warn("Can not get a TemplateProcessor with null name !!!");
            return null;
        }
        TemplateProcessorDescriptor desc = processorRegistry.getProcessorByName(name);
        if (desc != null) {
            return desc.getProcessor();
        } else {
            log.warn("Can not get a TemplateProcessor with name " + name);
            return null;
        }
    }

    public List<DocumentModel> getAvailableTemplateDocs(CoreSession session,
            String targetType) throws ClientException {

        StringBuffer sb = new StringBuffer("select * from TemplateSource");
        DocumentModelList templates = session.query(sb.toString());

        if (targetType==null) {
            return templates;
        } else {
            // post filter
            List<DocumentModel> filtredResult = new ArrayList<DocumentModel>();
            for (DocumentModel template : templates) {
                TemplateSourceDocument srcTmpl = template.getAdapter(TemplateSourceDocument.class);
                if (srcTmpl!=null) {
                    List<String> applicableTypes = srcTmpl.getApplicableTypes();
                    if (applicableTypes.size()==0 || applicableTypes.contains(targetType)) {
                        filtredResult.add(template);
                    }
                }
            }
            return filtredResult;
        }
    }

    public List<TemplateSourceDocument> getAvailableTemplates(
            CoreSession session, String targetType) throws ClientException {
        List<TemplateSourceDocument> result = new ArrayList<TemplateSourceDocument>();
        List<DocumentModel> filtredResult = getAvailableTemplateDocs(session, targetType);
        for (DocumentModel template : filtredResult) {
            TemplateSourceDocument srcTmpl = template.getAdapter(TemplateSourceDocument.class);
            if (srcTmpl==null) {
                log.error("Unable to find adapter on TemplateSourceDocument !");
            } else {
                result.add(srcTmpl);
            }
        }
        return result;
    }

    @Override
    public List<TemplateBasedDocument> getLinkedTemplateBasedDocuments(
            DocumentModel source) throws ClientException {

        StringBuffer sb = new StringBuffer("select * from Document where ");
        sb.append(TemplateBasedDocumentAdapterImpl.TEMPLATE_ID_PROP);
        sb.append(" = '");
        sb.append(source.getId());
        sb.append("'");
        DocumentModelList docs = source.getCoreSession().query(sb.toString());

        List<TemplateBasedDocument> result = new ArrayList<TemplateBasedDocument>();
        for (DocumentModel doc : docs) {
            TemplateBasedDocument templateBasedDocument = doc.getAdapter(TemplateBasedDocument.class);
            if (templateBasedDocument!=null) {
                result.add(templateBasedDocument);
            }
        }
        return result;
    }

    public Collection<TemplateProcessorDescriptor> getRegistredTemplateProcessors() {
        return processorRegistry.getRegistredProcessors();
    }

    public Map<String, String> getTypeMapping() {
        if (type2Template==null) {
            synchronized (this) {
                if (type2Template==null) {
                    type2Template = new ConcurrentHashMap<String, String>();
                    TemplateMappingFetcher fetcher = new TemplateMappingFetcher();
                    try {
                        fetcher.runUnrestricted();
                    } catch (ClientException e) {
                        log.error("Unable to fetch templates 2 types mapping", e);
                    }
                    type2Template.putAll(fetcher.getMapping());
                }
            }
        }
        return type2Template;
    }

    public void registerTypeMapping(DocumentModel doc) throws ClientException {
        TemplateSourceDocument tmpl = doc.getAdapter(TemplateSourceDocument.class);
        if (tmpl!=null) {
            Map<String, String> mapping = getTypeMapping();
            // unbind previous mapping
            List<String> boundTypes = new ArrayList<String>();
            for (String type : mapping.keySet()) {
                if (doc.getId().equals(mapping.get(type))) {
                    boundTypes.add(type);
                }
            }
            for (String type : boundTypes) {
                mapping.remove(type);
            }
            // rebind types (with override)
            for (String type : tmpl.getForcedTypes()) {
                String uidToClean = mapping.get(type);
                if (uidToClean!=null) {
                    new TemplateMappingRemover(doc.getCoreSession(), uidToClean, type).runUnrestricted();
                }
                mapping.put(type, doc.getId());
            }
        }
    }


    public DocumentModel makeTemplateBasedDocument(DocumentModel targetDoc, DocumentModel sourceTemplateDoc, boolean save) throws ClientException {
        targetDoc.addFacet(TemplateBasedDocumentAdapterImpl.TEMPLATEBASED_FACET);
        TemplateBasedDocument tmplBased = targetDoc.getAdapter(TemplateBasedDocument.class);
        // bind the template
        return tmplBased.setTemplate(sourceTemplateDoc, save);
    }
}
