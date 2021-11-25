/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
 *      Thibaud Arguillere
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
    protected Boolean ignorePrefix;

    /** @since 11.1 */
    @XNode("@readOnly")
    protected Boolean readOnly;

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

    /**
     * @deprecated since 11.1, use {@link #ignorePrefix()}
     */
    @Deprecated
    public Boolean getIgnorePrefix() {
        return ignorePrefix();
    }

    public boolean ignorePrefix() {
    	return !Boolean.FALSE.equals(ignorePrefix);
    }

    public boolean isReadOnly() {
    	return Boolean.TRUE.equals(readOnly);
    }

    @Override
    public String toString() {
        return "MetadataMappingDescriptor{id=" + id + ", processor=" + processor + ", blobXPath="
                + blobXPath  + ", ignorePrefix=" + ignorePrefix + ", readOnly=" + readOnly + '}';
    }
}
