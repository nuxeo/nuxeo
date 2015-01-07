/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     vpasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.binary.metadata.api.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.el.ExpressionFactoryImpl;
import org.nuxeo.binary.metadata.api.BinaryMetadataConstants;
import org.nuxeo.binary.metadata.api.BinaryMetadataException;
import org.nuxeo.binary.metadata.contribution.MetadataMappingDescriptor;
import org.nuxeo.binary.metadata.contribution.MetadataMappingRegistry;
import org.nuxeo.binary.metadata.contribution.MetadataProcessorDescriptor;
import org.nuxeo.binary.metadata.contribution.MetadataProcessorRegistry;
import org.nuxeo.binary.metadata.contribution.MetadataRuleDescriptor;
import org.nuxeo.binary.metadata.contribution.MetadataRuleRegistry;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ELActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.platform.el.ExpressionContext;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 7.1
 */
public class BinaryMetadataServiceImpl implements BinaryMetadataService {

    private static final Log log = LogFactory.getLog(BinaryMetadataServiceImpl.class);

    protected MetadataMappingRegistry mappingRegistry;

    protected MetadataProcessorRegistry processorRegistry;

    protected MetadataRuleRegistry ruleRegistry;

    protected Map<String, BinaryMetadataProcessor> binaryMetadataProcessorInstances;

    public BinaryMetadataServiceImpl() {
        this.mappingRegistry = new MetadataMappingRegistry();
        this.ruleRegistry = new MetadataRuleRegistry();
        this.processorRegistry = new MetadataProcessorRegistry();
        this.binaryMetadataProcessorInstances = new HashMap<>();
    }

    @Override
    public Map<String, Object> readMetadata(String processorName, Blob blob, List<String> metadataNames) {
        try {
            BinaryMetadataProcessor processor = (BinaryMetadataProcessor) getProcessor(processorName).newInstance();
            return processor.readMetadata(blob,metadataNames);
        } catch (InstantiationException | NoSuchMethodException | IllegalAccessException e) {
            throw new BinaryMetadataException(e);
        }
    }

    @Override
    public Map<String, Object> readMetadata(Blob blob, List<String> metadataNames) {
        try {
            BinaryMetadataProcessor processor = (BinaryMetadataProcessor) getProcessor(BinaryMetadataConstants.EXIF_TOOL_CONTRIBUTION_ID).newInstance();
            return processor.readMetadata(blob,metadataNames);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new BinaryMetadataException(e);
        }
    }

    @Override
    public Map<String, Object> readMetadata(Blob blob) {
        try {
            BinaryMetadataProcessor processor = (BinaryMetadataProcessor) getProcessor(BinaryMetadataConstants.EXIF_TOOL_CONTRIBUTION_ID).newInstance();
            return processor.readMetadata(blob);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new BinaryMetadataException(e);
        }
    }

    @Override
    public Map<String, Object> readMetadata(String processorName, Blob blob) {
        try {
            BinaryMetadataProcessor processor = (BinaryMetadataProcessor) getProcessor(processorName).newInstance();
            return processor.readMetadata(blob);
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException e) {
            throw new BinaryMetadataException(e);
        }
    }

    @Override
    public boolean writeMetadata(String processorName, Blob blob, Map<String, Object> metadata) {
        try {
            BinaryMetadataProcessor processor = (BinaryMetadataProcessor) getProcessor(processorName).newInstance();
            return processor.writeMetadata(blob, metadata);
        } catch (InstantiationException | NoSuchMethodException | IllegalAccessException e) {
            throw new BinaryMetadataException(e);
        }
    }

    @Override
    public boolean writeMetadata(Blob blob, Map<String, Object> metadata) {
        try {
            BinaryMetadataProcessor processor = (BinaryMetadataProcessor) getProcessor(BinaryMetadataConstants.EXIF_TOOL_CONTRIBUTION_ID).newInstance();
            return processor.writeMetadata(blob, metadata);
        } catch (InstantiationException | NoSuchMethodException | IllegalAccessException e) {
            throw new BinaryMetadataException(e);
        }
    }

    @Override
    public boolean writeMetadata(String processorName, Blob blob, String mappingDescriptorId, DocumentModel doc) {
        try {
            // Creating mapping properties Map.
            Map<String, Object> metadataMapping = new HashMap<>();
            MetadataMappingDescriptor mappingDescriptor = mappingRegistry.getMappingDescriptorMap().get(mappingDescriptorId);
            for (MetadataMappingDescriptor.MetadataDescriptor metadataDescriptor
                    : mappingDescriptor.getMetadataDescriptors()) {
                metadataMapping.put(metadataDescriptor.getName(),
                        doc.getPropertyValue(metadataDescriptor.getXpath()));
            }
            BinaryMetadataProcessor processor = (BinaryMetadataProcessor) getProcessor(processorName).newInstance();
            return processor.writeMetadata(blob, metadataMapping);
        } catch (InstantiationException | NoSuchMethodException | IllegalAccessException e) {
            throw new BinaryMetadataException(e);
        }

    }

    @Override
    public boolean writeMetadata(Blob blob, String mappingDescriptorId, DocumentModel doc) {
        try {
            // Creating mapping properties Map.
            Map<String, Object> metadataMapping = new HashMap<>();
            MetadataMappingDescriptor mappingDescriptor = mappingRegistry.getMappingDescriptorMap().get(mappingDescriptorId);
            for (MetadataMappingDescriptor.MetadataDescriptor metadataDescriptor
                    : mappingDescriptor.getMetadataDescriptors()) {
                metadataMapping.put(metadataDescriptor.getName(),
                        doc.getPropertyValue(metadataDescriptor.getXpath()));
            }
            BinaryMetadataProcessor processor = (BinaryMetadataProcessor) getProcessor(BinaryMetadataConstants.EXIF_TOOL_CONTRIBUTION_ID).newInstance();
            return processor.writeMetadata(blob, metadataMapping);
        } catch (InstantiationException | NoSuchMethodException | IllegalAccessException e) {
            throw new BinaryMetadataException(e);
        }
    }

    @Override
    public void writeMetadata(DocumentModel doc, CoreSession session) {
        // Check if rules applying for this document.
        ActionContext actionContext = createActionContext(doc);
        Set<MetadataRuleDescriptor> ruleDescriptors = checkFilter(actionContext);
        List<String> mappingDescriptorIds = new ArrayList<>();
        for(MetadataRuleDescriptor ruleDescriptor:ruleDescriptors){
            mappingDescriptorIds.addAll(ruleDescriptor
                    .getMetadataMappingIdDescriptors());
        }
        if (mappingDescriptorIds.isEmpty()) {
            return;
        }

        // For each mapping descriptors, overriding mapping document properties.
        for (String mappingDescriptorId : mappingDescriptorIds) {
            if (!mappingRegistry.getMappingDescriptorMap().containsKey(mappingDescriptorId)) {
                log.warn("Missing binary metadata descriptor with id '" + mappingDescriptorId
                        + "'. Or check your rule contribution with proper metadataMapping-id.");
                continue;
            }
            writeMetadata(doc, session, mappingDescriptorId);
        }
    }

    @Override
    public void writeMetadata(DocumentModel doc, CoreSession session, String mappingDescriptorId) {
        // Creating mapping properties Map.
        Map<String, String> metadataMapping = new HashMap<>();
        List<String> blobMetadata = new ArrayList<>();
        MetadataMappingDescriptor mappingDescriptor = mappingRegistry.getMappingDescriptorMap().get(mappingDescriptorId);

        // Extract blob from the contributed xpath
        Blob blob = doc.getProperty(mappingDescriptor.getBlobXPath()).getValue(Blob.class);
        if(blob!=null && mappingDescriptor.getMetadataDescriptors()!=null && !mappingDescriptor.getMetadataDescriptors().isEmpty()) {
            for (MetadataMappingDescriptor.MetadataDescriptor metadataDescriptor
                    : mappingDescriptor.getMetadataDescriptors()) {
                metadataMapping.put(metadataDescriptor.getName(),
                        metadataDescriptor.getXpath());
                blobMetadata.add(metadataDescriptor.getName());
            }

            // Extract metadata from binary.
            String processorId = mappingDescriptor.getProcessor();
            Map<String, Object> blobMetadataOutput;
            if (processorId != null) {
                blobMetadataOutput = readMetadata(processorId, blob, blobMetadata);
                
            } else {
                blobMetadataOutput = readMetadata(blob, blobMetadata);
            }

            // Write doc properties from outputs.
            for (Object metadata : blobMetadataOutput.keySet()) {
                doc.setPropertyValue(metadataMapping.get(metadata), blobMetadataOutput.get(metadata).toString());
            }
            if(session.exists(doc.getRef())) {
                session.saveDocument(doc);
            }
        }
    }

    /*--------------------- Event Service --------------------------*/

    @Override
    public void handleSyncUpdate(DocumentModel doc, DocumentEventContext docCtx) {
        LinkedList<MetadataMappingDescriptor> syncMappingDescriptors = getSyncMapping(doc, docCtx);
        if (syncMappingDescriptors != null) {
            handleUpdate(syncMappingDescriptors, doc, docCtx);
        }
    }

    @Override
    public void handleUpdate(LinkedList<MetadataMappingDescriptor> mappingDescriptors, DocumentModel doc,
            DocumentEventContext docCtx) {
        for (MetadataMappingDescriptor mappingDescriptor : mappingDescriptors) {
            Property fileProp = doc.getProperty(mappingDescriptor.getBlobXPath());
            boolean isDirtyMapping = isDirtyMapping(mappingDescriptor, doc);
            Blob blob = fileProp.getValue(Blob.class);
            if (blob != null) {
                if (fileProp.isDirty()) {
                    if (isDirtyMapping) {
                        // if Blob dirty and document metadata dirty, write metadata from doc to Blob
                        writeMetadata(fileProp.getValue(Blob.class), mappingDescriptor.getId(), doc);
                    } else {
                        // if Blob dirty and document metadata not dirty, write metadata from Blob to doc
                        writeMetadata(doc, docCtx.getCoreSession());
                    }
                } else {
                    if (isDirtyMapping) {
                        // if Blob not dirty and document metadata dirty, write metadata from doc to Blob
                        writeMetadata(fileProp.getValue(Blob.class), mappingDescriptor.getId(), doc);
                    }
                }
            }
        }
    }

    /*--------------------- Utils --------------------------*/

    /**
     * Check for each Binary Rule if the document is accepted or not.
     *
     * @return the list of metadata which should be processed sorted by rules order. (high to low priority)
     */
    protected Set<MetadataRuleDescriptor> checkFilter(ActionContext
            actionContext) {
        Set<MetadataRuleDescriptor> ruleDescriptors = new TreeSet<>();
        ActionManager actionService = Framework.getLocalService(ActionManager.class);
        for (String ruleDescriptorId : ruleRegistry.getMetadataRuleDescriptorMap().keySet()) {
            MetadataRuleDescriptor ruleDescriptor = ruleRegistry.getMetadataRuleDescriptorMap().get(ruleDescriptorId);
            if (!ruleDescriptor.getEnabled()
                    || !actionService.checkFilters(ruleDescriptor.getFilterIds(), actionContext)) {
                continue;
            }
            ruleDescriptors.add(ruleDescriptor);
        }
        return ruleDescriptors;
    }

    protected ActionContext createActionContext(DocumentModel doc) {
        ActionContext actionContext = new ELActionContext(new ExpressionContext(), new ExpressionFactoryImpl());
        actionContext.setCurrentDocument(doc);
        return actionContext;
    }

    protected Class getProcessor(String processorId) throws NoSuchMethodException {
        return binaryMetadataProcessorInstances.get(processorId).getClass();
    }

    /**
     * @return Dirty metadata from metadata mapping contribution and handle async processes.
     */
    public LinkedList<MetadataMappingDescriptor> getSyncMapping(DocumentModel doc, DocumentEventContext docCtx) {
        // Check if rules applying for this document.
        ActionContext actionContext = createActionContext(doc);
        Set<MetadataRuleDescriptor> ruleDescriptors = checkFilter(actionContext);
        Set<String> syncMappingDescriptorIds = new HashSet<>();
        HashSet<String> asyncMappingDescriptorIds = new HashSet<>();
        for(MetadataRuleDescriptor ruleDescriptor:ruleDescriptors){
            if(ruleDescriptor.getIsAsync()){
                asyncMappingDescriptorIds.addAll(ruleDescriptor.getMetadataMappingIdDescriptors());
            }
            syncMappingDescriptorIds.addAll(ruleDescriptor
                    .getMetadataMappingIdDescriptors());
        }

        // Handle async rules which should be taken into account in async listener.
        if(!asyncMappingDescriptorIds.isEmpty()){
            handleAsyncMapping(asyncMappingDescriptorIds, docCtx);
        }

        if (syncMappingDescriptorIds.isEmpty()) {
            return null;
        }
        return getMapping(syncMappingDescriptorIds);
    }

    /**
     * Handle Metadata Rules which should be executed within async listener.
     */
    protected void handleAsyncMapping(HashSet<String> asyncMappingDescriptorIds, DocumentEventContext docCtx) {
        docCtx.setProperty(BinaryMetadataConstants.ASYNC_MAPPING_RESULT, getMapping(asyncMappingDescriptorIds));
        EventService service = Framework.getService(EventService.class);
        service.fireEvent(BinaryMetadataConstants.ASYNC_BINARY_METADATA_EVENT, docCtx);
    }

    protected LinkedList<MetadataMappingDescriptor> getMapping(Set<String>
            mappingDescriptorIds){
        // For each mapping descriptors, store mapping.
        LinkedList<MetadataMappingDescriptor> mappingResult = new LinkedList<>();
        for (String mappingDescriptorId : mappingDescriptorIds) {
            if (!mappingRegistry.getMappingDescriptorMap().containsKey(mappingDescriptorId)) {
                log.warn("Missing binary metadata descriptor with id '" + mappingDescriptorId
                        + "'. Or check your rule contribution with proper metadataMapping-id.");
                continue;
            }
            mappingResult.add(mappingRegistry.getMappingDescriptorMap().get(mappingDescriptorId));
        }
        return mappingResult;
    }

    /**
     * Maps inspector only.
     */
    protected boolean isDirtyMapping(MetadataMappingDescriptor mappingDescriptor, DocumentModel doc) {
        Map<String, String> mappingResult = new HashMap<>();
        for (MetadataMappingDescriptor.MetadataDescriptor metadataDescriptor
                : mappingDescriptor.getMetadataDescriptors()) {
            mappingResult.put(metadataDescriptor.getXpath(),metadataDescriptor.getName());
        }
        // Returning only dirty properties
        HashMap<String, Object> resultDirtyMapping  =  new HashMap<>();
        for(String metadata: mappingResult.keySet()){
            Property property = doc.getProperty(metadata);
            if(property.isDirty()){
                resultDirtyMapping.put(mappingResult.get(metadata),doc.getPropertyValue(metadata));
            }
        }
        return !resultDirtyMapping.isEmpty();
    }

    /*--------------------- Registry Services -----------------------*/

    protected void addMappingContribution(MetadataMappingDescriptor contribution) {
        this.mappingRegistry.addContribution(contribution);
    }

    protected void addRuleContribution(MetadataRuleDescriptor contribution) {
        this.ruleRegistry.addContribution(contribution);
    }

    protected void addProcessorContribution(MetadataProcessorDescriptor contribution) {
        try {
            this.binaryMetadataProcessorInstances.put(contribution.getId(),contribution.getProcessorClass().newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new BinaryMetadataException(e);
        }
        this.processorRegistry.addContribution(contribution);
    }

    protected void removeMappingContribution(MetadataMappingDescriptor contribution) {
        this.mappingRegistry.removeContribution(contribution);
    }

    protected void removeRuleContribution(MetadataRuleDescriptor contribution) {
        this.ruleRegistry.removeContribution(contribution);
    }

    protected void removeProcessorContribution(MetadataProcessorDescriptor contribution) {
        this.processorRegistry.removeContribution(contribution);
    }

}
