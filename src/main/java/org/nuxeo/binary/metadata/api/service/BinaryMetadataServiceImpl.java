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
    public void readMetadata(DocumentModel doc) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeMetadata(DocumentModel doc) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> readMetadata(String processorName, Blob blob, List<String> metadataNames) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, Object> readMetadata(Blob blob, List<String> metadataNames) {
        try {
            Class[] params = {Blob.class, List.class};
            Method method = getProcessorMethod(BinaryMetadataConstants.EXIF_TOOL_CONTRIBUTION_ID, BinaryMetadataConstants.READ_METADATA_METHOD, params);
            return processorMethodInvoker(BinaryMetadataConstants.EXIF_TOOL_CONTRIBUTION_ID, method, blob, metadataNames);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new BinaryMetadataException(e);
        }
    }

    @Override
    public Map<String, Object> readMetadata(Blob blob) {
        try {
            Class[] paramBlob = {Blob.class};
            Method method = getProcessorMethod(BinaryMetadataConstants.EXIF_TOOL_CONTRIBUTION_ID, BinaryMetadataConstants.READ_METADATA_METHOD, paramBlob);
            return processorMethodInvoker(BinaryMetadataConstants.EXIF_TOOL_CONTRIBUTION_ID, method, blob);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new BinaryMetadataException(e);
        }
    }

    @Override
    public Map<String, Object> readMetadata(String processorName, Blob blob) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeMetadata(String processorName, Blob blob, Map<String, Object> metadata) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeMetadata(Blob blob, Map<String, Object> metadata) {

    }

    protected Map<String, Object> processorMethodInvoker(String processorId, Method method, Object... args)
            throws IllegalAccessException, InvocationTargetException {
        return (Map<String, Object>) method.invoke(binaryMetadataProcessorInstances.get(processorId), args);
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
