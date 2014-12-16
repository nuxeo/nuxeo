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
public class BinaryMetadataServiceImpl implements BinaryMetadataService, BinaryMetadataRegistryService {

    private static final Log log = LogFactory.getLog(BinaryMetadataServiceImpl.class);

    protected MetadataMappingRegistry mappingRegistry;

    protected MetadataProcessorRegistry processorRegistry;

    protected MetadataRuleRegistry ruleRegistry;

    public BinaryMetadataServiceImpl() {
        this.mappingRegistry = new MetadataMappingRegistry();
        this.ruleRegistry = new MetadataRuleRegistry();
        this.processorRegistry = new MetadataProcessorRegistry();
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
    public Map<String, String> readMetadata(String processorName, Blob blob, List<String> metadataNames) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> readMetadata(Blob blob, List<String> metadataNames) {
        return null;
    }

    @Override
    public Map<String, Object> readMetadata(Blob blob) {
        try {
            Class[] paramBlob = new Class[1];
            paramBlob[0] = Blob.class;
            BinaryMetadataProcessor binaryMetadataProcessor = processorRegistry.getProcessorDescriptorMap().get(
                    BinaryMetadataConstants.EXIF_TOOL_CONTRIBUTION_ID).getProcessorClass().newInstance();
            Method method = binaryMetadataProcessor.getClass().getDeclaredMethod("readMetadata", paramBlob);
            return (Map<String, Object>) method.invoke(binaryMetadataProcessor, blob);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
            throw new BinaryMetadataException(e);
        }
    }

    @Override
    public Map<String, String> readMetadata(String processorName, Blob blob) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeMetadata(String processorName, Blob blob, Map<String, String> metadata) {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeMetadata(Blob blob, Map<String, String> metadata) {

    }

    /*--------------------- Registry Service -----------------------*/

    @Override
    public void addMappingContribution(MetadataMappingDescriptor contribution) {
        this.mappingRegistry.addContribution(contribution);
    }

    @Override
    public void addRuleContribution(MetadataRuleDescriptor contribution) {
        this.ruleRegistry.addContribution(contribution);
    }

    @Override
    public void addProcessorContribution(MetadataProcessorDescriptor contribution) {
        this.processorRegistry.addContribution(contribution);
    }

    @Override
    public void removeMappingContribution(MetadataMappingDescriptor contribution) {
        this.mappingRegistry.removeContribution(contribution);
    }

    @Override
    public void removeRuleContribution(MetadataRuleDescriptor contribution) {
        this.ruleRegistry.removeContribution(contribution);
    }

    @Override
    public void removeProcessorContribution(MetadataProcessorDescriptor contribution) {
        this.processorRegistry.removeContribution(contribution);
    }

}
