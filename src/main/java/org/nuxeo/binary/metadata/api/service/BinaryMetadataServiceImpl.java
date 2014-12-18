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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
            Class[] params = {Blob.class, List.class};
            Method method = getProcessorMethod(processorName, BinaryMetadataConstants.READ_METADATA_METHOD, params);
            return (Map<String, Object>) processorMethodInvoker(processorName, method, blob, metadataNames);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new BinaryMetadataException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> readMetadata(Blob blob, List<String> metadataNames) {
        try {
            Class[] params = {Blob.class, List.class};
            Method method = getProcessorMethod(BinaryMetadataConstants.EXIF_TOOL_CONTRIBUTION_ID, BinaryMetadataConstants.READ_METADATA_METHOD, params);
            return (Map<String, Object>) processorMethodInvoker(BinaryMetadataConstants.EXIF_TOOL_CONTRIBUTION_ID, method, blob, metadataNames);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new BinaryMetadataException(e);
        }
    }

    @Override
    public Map<String, Object> readMetadata(Blob blob) {
        try {
            Class[] paramBlob = {Blob.class};
            Method method = getProcessorMethod(BinaryMetadataConstants.EXIF_TOOL_CONTRIBUTION_ID, BinaryMetadataConstants.READ_METADATA_METHOD, paramBlob);
            return (Map<String, Object>) processorMethodInvoker(BinaryMetadataConstants.EXIF_TOOL_CONTRIBUTION_ID, method, blob);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new BinaryMetadataException(e);
        }
    }

    @Override
    public Map<String, Object> readMetadata(String processorName, Blob blob) {
        try {
            Class[] paramBlob = {Blob.class};
            Method method = getProcessorMethod(processorName, BinaryMetadataConstants.READ_METADATA_METHOD, paramBlob);
            return (Map<String, Object>) processorMethodInvoker(processorName, method, blob);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new BinaryMetadataException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean writeMetadata(String processorName, Blob blob, Map<String, Object> metadata) {
        try {
            Class[] params = {Blob.class, Map.class};
            Method method = getProcessorMethod(processorName, BinaryMetadataConstants.WRITE_METADATA_METHOD, params);
            return (boolean) processorMethodInvoker(processorName, method, blob, metadata);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new BinaryMetadataException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean writeMetadata(Blob blob, Map<String, Object> metadata) {
        try {
            Class[] params = {Blob.class, Map.class};
            Method method = getProcessorMethod(BinaryMetadataConstants.EXIF_TOOL_CONTRIBUTION_ID, BinaryMetadataConstants.WRITE_METADATA_METHOD, params);
            return (boolean) processorMethodInvoker(BinaryMetadataConstants.EXIF_TOOL_CONTRIBUTION_ID, method, blob, metadata);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new BinaryMetadataException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeMetadata(DocumentModel doc) {
        CoreSession session = doc.getCoreSession();
        MetadataMappingDescriptor desc = mappingRegistry
                .getProcessorDescriptorMap().get(doc.getType());
        Blob blob = doc.getProperty(desc.getBlobXPath()).getValue(Blob.class);
        String processorId = desc.getProcessor();
        Map<String, Object> metadataMapping = new HashMap<>();
        List<String> blobMetadata = new ArrayList<>();
        for (MetadataMappingDescriptor.MetadataDescriptor metadataDescriptor
                : desc.getMetadataDescriptors()) {
            metadataMapping.put(metadataDescriptor.getName(),
                    metadataDescriptor.getXpath());
            blobMetadata.add(metadataDescriptor.getName());
        }

        // Extract metadata from binary
        Map<String,Object> blobMetadataOutput;
        if (processorId != null) {
            blobMetadataOutput = readMetadata(processorId, blob, blobMetadata);
        } else {
            blobMetadataOutput = readMetadata(blob, blobMetadata);
        }

        // Write doc properties from outputs
        for(Object metadata: blobMetadataOutput.keySet()){
            doc.setPropertyValue(metadataMapping.get(metadata).toString(),blobMetadataOutput.get(metadata).toString());
        }
        session.saveDocument(doc);
        session.save();
    }

    protected Object processorMethodInvoker(String processorId, Method method, Object... args)
            throws IllegalAccessException, InvocationTargetException {
        return method.invoke(binaryMetadataProcessorInstances.get(processorId), args);
    }

    protected Method getProcessorMethod(String processorId, String methodId, Class[] params)
            throws NoSuchMethodException {
        return binaryMetadataProcessorInstances.get(processorId).getClass().getDeclaredMethod(methodId, params);
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
