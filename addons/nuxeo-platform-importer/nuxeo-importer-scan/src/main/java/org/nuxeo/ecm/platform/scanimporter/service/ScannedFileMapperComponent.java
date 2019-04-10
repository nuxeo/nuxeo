/*
 * (C) Copyright 2006-2012 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.scanimporter.service;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.tree.DefaultElement;
import org.jaxen.XPath;
import org.jaxen.dom4j.Dom4jXPath;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.scanimporter.processor.DocumentTypeMapper;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 *
 * Component to provide service logic : - meta-data parsing - configuration
 * management - extension points
 *
 * @author Thierry Delprat
 *
 */
public class ScannedFileMapperComponent extends DefaultComponent implements
        ScannedFileMapperService {

    private static final Log log = LogFactory.getLog(ScannedFileMapperComponent.class);

    public static final String MAPPING_EP = "mapping";

    public static final String CONFIG_EP = "config";

    protected ScanFileMappingDescriptor mappingDesc = null;

    protected ImporterConfig config = null;

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {

        if (MAPPING_EP.equals(extensionPoint)) {
            mappingDesc = (ScanFileMappingDescriptor) contribution;
        } else if (CONFIG_EP.equals(extensionPoint)) {
            config = (ImporterConfig) contribution;
        }
    }

    @Override
    public ScanFileBlobHolder parseMetaData(File xmlFile) throws Exception {

        Map<String, Serializable> data = new HashMap<String, Serializable>();

        if (mappingDesc == null) {
            return null;
        }

        String xmlData = FileUtils.readFile(xmlFile);

        Document xmlDoc = DocumentHelper.parseText(xmlData);

        for (ScanFileFieldMapping fieldMap : mappingDesc.getFieldMappings()) {

            XPath xpath = new Dom4jXPath(fieldMap.getSourceXPath());
            List nodes = xpath.selectNodes(xmlDoc);
            if (nodes.size() == 1) {
                DefaultElement elem = (DefaultElement) nodes.get(0);
                String value = null;
                if ("TEXT".equals(fieldMap.getSourceAttribute())) {
                    value = elem.getText();
                } else {
                    value = elem.attribute(fieldMap.getSourceAttribute()).getValue();
                }

                String target = fieldMap.getTargetXPath();
                if ("string".equalsIgnoreCase(fieldMap.getTargetType())) {
                    data.put(target, value);
                } else if ("integer".equalsIgnoreCase(fieldMap.getTargetType())) {
                    data.put(target, Integer.parseInt(value));
                } else if ("double".equalsIgnoreCase(fieldMap.getTargetType())) {
                    data.put(target, Double.parseDouble(value));
                } else if ("date".equalsIgnoreCase(fieldMap.getTargetType())) {
                    data.put(target, fieldMap.getDateFormat().parse(value));
                }
            }
        }

        List<Blob> blobs = new ArrayList<Blob>();

        for (ScanFileBlobMapping blobMap : mappingDesc.getBlobMappings()) {
            XPath xpath = new Dom4jXPath(blobMap.getSourceXPath());
            List nodes = xpath.selectNodes(xmlDoc);
            for (Object node : nodes) {
                DefaultElement elem = (DefaultElement) node;
                String filePath = elem.attributeValue(blobMap.getSourcePathAttribute());
                String fileName = elem.attributeValue(blobMap.getSourceFilenameAttribute());

                // Mainly for tests
                if (filePath.startsWith("$TMP")) {
                    filePath = filePath.replace("$TMP",
                            Framework.getProperty("nuxeo.import.tmpdir"));
                }

                File file = new File(filePath);
                if (file.exists()) {
                    Blob blob = new FileBlob(file);
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
                    log.error("File " + file.getAbsolutePath()
                            + " is referenced by " + xmlFile.getAbsolutePath()
                            + " but was not found");
                }
            }
        }

        String targetType = getTargetLeafType();
        DocumentTypeMapper mapper = mappingDesc.getTargetLeafTypeMapper();
        if (mapper != null) {
            targetType = mapper.getTargetDocumentType(xmlDoc, xmlFile);
        }
        ScanFileBlobHolder bh = new ScanFileBlobHolder(blobs, data, targetType);
        return bh;
    }

    public ScanFileMappingDescriptor getMappingDesc() {
        return mappingDesc;
    }

    @Override
    public String getTargetContainerType() {
        if (mappingDesc == null) {
            return ScanFileMappingDescriptor.DEFAULT_CONTAINER_TYPE;
        }
        return mappingDesc.getTargetContainerType();
    }

    @Override
    public String getTargetLeafType() {
        if (mappingDesc == null) {
            return ScanFileMappingDescriptor.DEFAULT_LEAF_TYPE;
        }
        return mappingDesc.getTargetLeafType();
    }

    @Override
    public ImporterConfig getImporterConfig() {
        return config;
    }
}
