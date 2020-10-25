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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.scanimporter.processor.DocumentTypeMapper;

/**
 * Top level descriptor for mapping
 *
 * @author Thierry Delprat
 */
@XObject("mapping")
public class ScanFileMappingDescriptor {

    public static final String DEFAULT_CONTAINER_TYPE = "Folder";

    public static final String DEFAULT_LEAF_TYPE = "File";

    private static final Log log = LogFactory.getLog(ScanFileMappingDescriptor.class);

    @XNode("targetContainerType")
    protected String targetContainerType = DEFAULT_CONTAINER_TYPE;

    @XNode("targetLeafType")
    protected String targetLeafType = DEFAULT_LEAF_TYPE;

    @XNode("targetLeafTypeMapper")
    protected Class<DocumentTypeMapper> mapperClass;

    protected DocumentTypeMapper leafTypeMapper = null;

    @XNodeList(value = "fieldMappings/fieldMapping", type = ArrayList.class, componentType = ScanFileFieldMapping.class)
    private List<ScanFileFieldMapping> fieldMappings;

    @XNodeList(value = "blobMappings/blobMapping", type = ArrayList.class, componentType = ScanFileBlobMapping.class)
    private List<ScanFileBlobMapping> blobMappings;

    public List<ScanFileFieldMapping> getFieldMappings() {
        return fieldMappings;
    }

    public List<ScanFileBlobMapping> getBlobMappings() {
        return blobMappings;
    }

    public String getTargetContainerType() {
        return targetContainerType;
    }

    public String getTargetLeafType() {
        return targetLeafType;
    }

    public DocumentTypeMapper getTargetLeafTypeMapper() {
        if (mapperClass == null) {
            return null;
        }
        if (leafTypeMapper == null) {
            try {
                leafTypeMapper = mapperClass.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                log.error("Unable to instanciate mapper class", e);
            }
        }
        return leafTypeMapper;
    }

}
