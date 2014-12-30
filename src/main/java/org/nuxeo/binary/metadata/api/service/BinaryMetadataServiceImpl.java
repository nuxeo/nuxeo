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
import java.util.List;
import java.util.Map;

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
import org.nuxeo.ecm.platform.actions.ActionContext;
import org.nuxeo.ecm.platform.actions.ELActionContext;
import org.nuxeo.ecm.platform.actions.ejb.ActionManager;
import org.nuxeo.ecm.platform.el.ExpressionContext;
import org.nuxeo.runtime.api.Framework;

/**
 * {@inheritDoc}
 *
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> readMetadata(String processorName, Blob blob, List<String> metadataNames) {
        try {
            BinaryMetadataProcessor processor = (BinaryMetadataProcessor) getProcessor(processorName).newInstance();
            return processor.readMetadata(blob,metadataNames);
        } catch (InstantiationException | NoSuchMethodException | IllegalAccessException e) {
            throw new BinaryMetadataException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
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

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean writeMetadata(String processorName, Blob blob, Map<String, Object> metadata) {
        try {
            BinaryMetadataProcessor processor = (BinaryMetadataProcessor) getProcessor(processorName).newInstance();
            return processor.writeMetadata(blob, metadata);
        } catch (InstantiationException | NoSuchMethodException | IllegalAccessException e) {
            throw new BinaryMetadataException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean writeMetadata(Blob blob, Map<String, Object> metadata) {
        try {
            BinaryMetadataProcessor processor = (BinaryMetadataProcessor) getProcessor(BinaryMetadataConstants.EXIF_TOOL_CONTRIBUTION_ID).newInstance();
            return processor.writeMetadata(blob, metadata);
        } catch (InstantiationException | NoSuchMethodException | IllegalAccessException e) {
            throw new BinaryMetadataException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeMetadata(DocumentModel doc, CoreSession session) {
        // Check if rules applying for this document.
        ActionContext actionContext = createActionContext(doc);
        List<String> mappingDescriptorIds = checkFilter(actionContext);
        if (mappingDescriptorIds == null || mappingDescriptorIds.isEmpty()) {
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

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeMetadata(DocumentModel doc, CoreSession session, String mappingDescriptorId) {
        // Creating mapping properties Map.
        Map<String, Object> metadataMapping = new HashMap<>();
        List<String> blobMetadata = new ArrayList<>();
        MetadataMappingDescriptor mappingDescriptor = mappingRegistry.getMappingDescriptorMap().get(mappingDescriptorId);
        for (MetadataMappingDescriptor.MetadataDescriptor metadataDescriptor
                : mappingDescriptor.getMetadataDescriptors()) {
            metadataMapping.put(metadataDescriptor.getName(),
                    metadataDescriptor.getXpath());
            blobMetadata.add(metadataDescriptor.getName());
        }

        // Extract metadata from binary.
        Blob blob = doc.getProperty(mappingDescriptor.getBlobXPath()).getValue(Blob.class);
        String processorId = mappingDescriptor.getProcessor();
        Map<String, Object> blobMetadataOutput;
        if (processorId != null) {
            blobMetadataOutput = readMetadata(processorId, blob, blobMetadata);
        } else {
            blobMetadataOutput = readMetadata(blob, blobMetadata);
        }

        // Write doc properties from outputs.
        for (Object metadata : blobMetadataOutput.keySet()) {
            doc.setPropertyValue(metadataMapping.get(metadata).toString(), blobMetadataOutput.get(metadata).toString());
        }
        session.saveDocument(doc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getMappingMetadata(DocumentModel doc) {
        // Check if rules applying for this document.
        ActionContext actionContext = createActionContext(doc);
        List<String> mappingDescriptorIds = checkFilter(actionContext);
        if (mappingDescriptorIds == null || mappingDescriptorIds.isEmpty()) {
            return null;
        }
        // For each mapping descriptors, store mapping.
        Map<String, String> mappingResult = new HashMap<>();
        for (String mappingDescriptorId : mappingDescriptorIds) {
            if (!mappingRegistry.getMappingDescriptorMap().containsKey(mappingDescriptorId)) {
                log.warn("Missing binary metadata descriptor with id '" + mappingDescriptorId
                        + "'. Or check your rule contribution with proper metadataMapping-id.");
                continue;
            }
            // Creating mapping properties.
            MetadataMappingDescriptor mappingDescriptor = mappingRegistry.getMappingDescriptorMap().get(mappingDescriptorId);
            for (MetadataMappingDescriptor.MetadataDescriptor metadataDescriptor
                    : mappingDescriptor.getMetadataDescriptors()) {
                mappingResult.put(metadataDescriptor.getXpath(),metadataDescriptor.getName());
            }
        }
        return mappingResult;
    }

    protected List<String> checkFilter(ActionContext actionContext) {
        ActionManager actionService = Framework.getLocalService(ActionManager.class);
        for (String ruleDescriptorId : ruleRegistry.getMetadataRuleDescriptorMap().keySet()) {
            MetadataRuleDescriptor ruleDescriptor = ruleRegistry.getMetadataRuleDescriptorMap().get(ruleDescriptorId);
            if (!ruleDescriptor.getEnabled()
                    || !actionService.checkFilters(ruleDescriptor.getFilterIds(), actionContext)) {
                continue;
            }
            return ruleDescriptor.getMetadataMappingIdDescriptors();
        }
        return null;
    }

    protected ActionContext createActionContext(DocumentModel doc) {
        ActionContext actionContext = new ELActionContext(new ExpressionContext(), new ExpressionFactoryImpl());
        actionContext.setCurrentDocument(doc);
        return actionContext;
    }

    protected Class getProcessor(String processorId) throws NoSuchMethodException {
        return binaryMetadataProcessorInstances.get(processorId).getClass();
    }

    /*--------------------- Registry Service -----------------------*/

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
