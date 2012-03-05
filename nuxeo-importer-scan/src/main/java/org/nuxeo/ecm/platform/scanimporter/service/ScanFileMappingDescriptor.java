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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.scanimporter.processor.DocumentTypeMapper;

/**
 *
 * Top level descriptor for mapping
 *
 * @author Thierry Delprat
 *
 */
@XObject("mapping")
public class ScanFileMappingDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

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
                leafTypeMapper = mapperClass.newInstance();
            } catch (Exception e) {
                log.error("Unable to instanciate mapper class", e);
            }
        }
        return leafTypeMapper;
    }

}
