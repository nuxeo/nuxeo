/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.scanimporter.service;

import static java.util.Objects.requireNonNullElse;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.tree.DefaultElement;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.platform.importer.source.FileSourceNode;
import org.nuxeo.ecm.platform.scanimporter.processor.DocumentTypeMapper;
import org.nuxeo.ecm.platform.scanimporter.processor.ScanedFileFactory;
import org.nuxeo.ecm.platform.scanimporter.processor.ScanedFileSourceNode;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Component to provide service logic : - meta-data parsing - configuration management - extension points
 *
 * @author Thierry Delprat
 */
public class ScannedFileMapperComponent extends DefaultComponent implements ScannedFileMapperService {

    private static final Log log = LogFactory.getLog(ScannedFileMapperComponent.class);

    public static final String MAPPING_EP = "mapping";

    public static final String CONFIG_EP = "config";

    /** @since 11.5 */
    public static final String GLOBAL_CONFIG_EP = "globalConfig";

    /** @since 11.5 */
    @XNode("@factoryClass")
    protected Class<? extends ScanedFileFactory> factoryClass;

    /** @since 11.5 */
    @XNode("@sourceNodeClass")
    protected Class<? extends FileSourceNode> sourceNodeClass;

    @Override
    public void start(ComponentContext context) {
        this.<ImporterGlobalConfig> getRegistryContribution(GLOBAL_CONFIG_EP).ifPresentOrElse(desc -> {
            this.factoryClass = requireNonNullElse(desc.getFactoryClass(), ScanedFileFactory.class);
            this.sourceNodeClass = requireNonNullElse(desc.getSourceNodeClass(), ScanedFileSourceNode.class);
        }, () -> {
            this.factoryClass = ScanedFileFactory.class;
            this.sourceNodeClass = ScanedFileSourceNode.class;
        });
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        this.factoryClass = null;
        this.sourceNodeClass = null;
    }

    @Override
    public ScanFileBlobHolder parseMetaData(File xmlFile) throws IOException {

        Map<String, Serializable> data = new HashMap<>();

        ScanFileMappingDescriptor mappingDesc = getMappingDesc();
        if (mappingDesc == null) {
            return null;
        }

        String xmlData = FileUtils.readFileToString(xmlFile, StandardCharsets.UTF_8);

        Document xmlDoc;
        try {
            xmlDoc = DocumentHelper.parseText(xmlData);
        } catch (DocumentException e) {
            throw new IOException(e);
        }

        for (ScanFileFieldMapping fieldMap : mappingDesc.getFieldMappings()) {

            List<?> nodes;
            nodes = xmlDoc.selectNodes(fieldMap.getSourceXPath());
            if (nodes.size() == 1) {
                DefaultElement elem = (DefaultElement) nodes.get(0);
                String value;
                if ("TEXT".equals(fieldMap.getSourceAttribute())) {
                    value = elem.getText();
                } else {
                    value = elem.attribute(fieldMap.getSourceAttribute()).getValue();
                }

                String target = fieldMap.getTargetXPath();
                if ("string".equalsIgnoreCase(fieldMap.getTargetType())) {
                    data.put(target, value);
                    continue;
                } else if ("integer".equalsIgnoreCase(fieldMap.getTargetType())) {
                    data.put(target, Integer.valueOf(value));
                    continue;
                } else if ("double".equalsIgnoreCase(fieldMap.getTargetType())) {
                    data.put(target, Double.valueOf(value));
                    continue;
                } else if ("date".equalsIgnoreCase(fieldMap.getTargetType())) {
                    try {
                        data.put(target, fieldMap.getDateFormat().parse(value));
                    } catch (ParseException e) {
                        throw new IOException(e);
                    }
                    continue;
                } else if ("boolean".equalsIgnoreCase(fieldMap.getTargetType())) {
                    data.put(target, Boolean.valueOf(value));
                    continue;
                }
                log.error("Unknown target type, please look the scan importer configuration: "
                        + fieldMap.getTargetType());
            }
            log.error("Mulliple or no element(s) found for: " + fieldMap.sourceXPath + " for "
                    + xmlFile.getAbsolutePath());

        }

        List<Blob> blobs = new ArrayList<>();

        for (ScanFileBlobMapping blobMap : mappingDesc.getBlobMappings()) {
            List<?> nodes;
            nodes = xmlDoc.selectNodes(blobMap.getSourceXPath());
            for (Object node : nodes) {
                DefaultElement elem = (DefaultElement) node;
                String filePath = elem.attributeValue(blobMap.getSourcePathAttribute());
                String fileName = elem.attributeValue(blobMap.getSourceFilenameAttribute());

                // Mainly for tests
                if (filePath.startsWith("$TMP")) {
                    filePath = filePath.replace("$TMP", Framework.getProperty("nuxeo.import.tmpdir"));
                }

                File file = new File(filePath);
                if (file.exists()) {
                    Blob blob = Blobs.createBlob(file);
                    if (fileName != null) {
                        blob.setFilename(fileName);
                    } else {
                        blob.setFilename(file.getName());
                    }
                    String target = blobMap.getTargetXPath();
                    if (target == null) {
                        blobs.add(blob);
                    } else {
                        data.put(target, (Serializable) blob);
                    }
                } else {
                    log.error("File " + file.getAbsolutePath() + " is referenced by " + xmlFile.getAbsolutePath()
                            + " but was not found");
                }
            }
        }

        String targetType = getTargetLeafType();
        DocumentTypeMapper mapper = mappingDesc.getTargetLeafTypeMapper();
        if (mapper != null) {
            targetType = mapper.getTargetDocumentType(xmlDoc, xmlFile);
        }
        return new ScanFileBlobHolder(blobs, data, targetType);
    }

    public ScanFileMappingDescriptor getMappingDesc() {
        return (ScanFileMappingDescriptor) getRegistryContribution(MAPPING_EP).orElse(null);
    }

    @Override
    public String getTargetContainerType() {
        ScanFileMappingDescriptor mappingDesc = getMappingDesc();
        if (mappingDesc == null) {
            return ScanFileMappingDescriptor.DEFAULT_CONTAINER_TYPE;
        }
        return mappingDesc.getTargetContainerType();
    }

    @Override
    public String getTargetLeafType() {
        ScanFileMappingDescriptor mappingDesc = getMappingDesc();
        if (mappingDesc == null) {
            return ScanFileMappingDescriptor.DEFAULT_LEAF_TYPE;
        }
        return mappingDesc.getTargetLeafType();
    }

    @Override
    public ImporterConfig getImporterConfig() {
        return (ImporterConfig) getRegistryContribution(CONFIG_EP).orElse(null);
    }

    @Override
    public Class<? extends ScanedFileFactory> getFactoryClass() {
        return factoryClass;
    }

    @Override
    public Class<? extends FileSourceNode> getSourceNodeClass() {
        return sourceNodeClass;
    }

}
