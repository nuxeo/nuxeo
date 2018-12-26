/*
 * (C) Copyright 2014-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Maxime Hilaire
 *     Florent Guillaume
 */
package org.nuxeo.ecm.directory.doc;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.directory.BaseDirectoryDescriptor;

/**
 * Descriptor for a {@link DocDirectory}.
 *
 * @since 8.2
 */
@XObject(value = "directory")
public class DocDirectoryDescriptor extends BaseDirectoryDescriptor {

    protected static final Log log = LogFactory.getLog(DocDirectoryDescriptor.class);

    @XNode("repositoryName")
    protected String repositoryName;

    @XNode("directoriesPath")
    protected String directoriesPath;

    @XNode("directoriesType")
    protected String directoriesType;

    @XNode("directoryType")
    protected String directoryType;

    @XNode("docType")
    protected String docType;

    @XNode("querySizeLimit")
    public Integer querySizeLimit;

    @Override
    public DocDirectoryDescriptor clone() {
        return (DocDirectoryDescriptor) super.clone();
    }

    @Override
    public void merge(BaseDirectoryDescriptor other) {
        super.merge(other);
        if (other instanceof DocDirectoryDescriptor) {
            merge((DocDirectoryDescriptor) other);
        }
    }

    protected void merge(DocDirectoryDescriptor other) {
        if (other.repositoryName != null) {
            repositoryName = other.repositoryName;
        }
        if (other.directoriesPath != null) {
            directoriesPath = other.directoriesPath;
        }
        if (other.directoriesType != null) {
            directoriesType = other.directoriesType;
        }
        if (other.directoryType != null) {
            directoryType = other.directoryType;
        }
        if (other.docType != null) {
            docType = other.docType;
        }
        if (other.querySizeLimit != null) {
            querySizeLimit = other.querySizeLimit;
        }
    }

    @Override
    public DocDirectory newDirectory() {
        return new DocDirectory(this);
    }

}
