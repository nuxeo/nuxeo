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
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.binary.metadata.internals;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @since 7.1
 */
@XObject("metadataMapping")
public class MetadataMappingDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@id")
    protected String id;

    @XNode("@processor")
    protected String processor;

    @XNode("@blobXPath")
    protected String blobXPath;

    @XNode("@ignorePrefix")
    protected Boolean ignorePrefix = true;

    @XNodeList(value = "metadata", componentType = MetadataDescriptor.class, type = ArrayList.class)
    protected List<MetadataDescriptor> metadataDescriptors;

    public List<MetadataDescriptor> getMetadataDescriptors() {
        return metadataDescriptors;
    }

    @XObject("metadata")
    public static class MetadataDescriptor implements Serializable {

        private static final long serialVersionUID = 1L;

        @XNode("@name")
        protected String name;

        @XNode("@xpath")
        protected String xpath;

        public String getXpath() {
            return xpath;
        }

        public String getName() {
            return name;
        }

    }

    public String getId() {
        return id;
    }

    public String getProcessor() {
        return processor;
    }

    public String getBlobXPath() {
        return blobXPath;
    }

    public Boolean getIgnorePrefix() {
        return ignorePrefix;
    }
}
