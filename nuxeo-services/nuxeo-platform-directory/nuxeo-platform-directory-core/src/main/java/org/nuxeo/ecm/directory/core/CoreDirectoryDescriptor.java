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
package org.nuxeo.ecm.directory.core;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.directory.BaseDirectoryDescriptor;
import org.nuxeo.runtime.api.Framework;

/**
 * Descriptor for a {@link CoreDirectory}.
 *
 * @since 8.2
 */
@XObject(value = "directory")
public class CoreDirectoryDescriptor extends BaseDirectoryDescriptor {

    protected static final Log log = LogFactory.getLog(CoreDirectoryDescriptor.class);

    public static final String DEFAULT_CREATE_PATH = "/";

    public static final boolean DEFAULT_CAN_CREATE_ROOT_FOLDER = true;

    @XObject(value = "acl")
    public static class ACLDescriptor implements Cloneable {

        @XNode("@userOrGroupName")
        public String userOrGroupName;

        @XNode("@privilege")
        public String privilege;

        @XNode("@granted")
        public boolean granted = false;

    }

    @XNode("docType")
    protected String docType;

    @XNode("querySizeLimit")
    public Integer querySizeLimit;

    @XNode("repositoryName")
    protected String repositoryName;

    @XNode("createPath")
    protected String createPath;

    public String getCreatePath() {
        return createPath == null ? DEFAULT_CREATE_PATH : createPath;
    }

    @XNode("canCreateRootFolder")
    protected Boolean canCreateRootFolder;

    public boolean canCreateRootFolder() {
        return canCreateRootFolder == null ? DEFAULT_CAN_CREATE_ROOT_FOLDER : canCreateRootFolder.booleanValue();
    }

    @XNodeMap(value = "fieldMapping", key = "@name", type = HashMap.class, componentType = String.class)
    public Map<String, String> fieldMapping = new HashMap<String, String>();

    @XNodeList(value = "acl", type = ACLDescriptor[].class, componentType = ACLDescriptor.class)
    protected ACLDescriptor[] acls;

    @Override
    public CoreDirectoryDescriptor clone() {
        CoreDirectoryDescriptor clone = (CoreDirectoryDescriptor) super.clone();
        // basic fields are already copied by super.clone()
        clone.fieldMapping = fieldMapping;
        if (acls != null) {
            clone.acls = acls.clone();
        }
        return clone;
    }

    public String getRepositoryName() {
        if (StringUtils.isBlank(repositoryName)) {
            repositoryName = Framework.getService(RepositoryManager.class).getDefaultRepositoryName();
        }
        return repositoryName;
    }

    @Override
    public void merge(BaseDirectoryDescriptor other) {
        super.merge(other);
        if (other instanceof CoreDirectoryDescriptor) {
            merge((CoreDirectoryDescriptor) other);
        }
    }

    protected void merge(CoreDirectoryDescriptor other) {
        if (other.docType != null) {
            docType = other.docType;
        }
        if (other.querySizeLimit != null) {
            querySizeLimit = other.querySizeLimit;
        }
        if (other.repositoryName != null) {
            repositoryName = other.repositoryName;
        }
        if (other.createPath != null) {
            createPath = other.createPath;
        }
        if (other.docType != null) {
            docType = other.docType;
        }
        if (other.fieldMapping != null) {
            fieldMapping = other.fieldMapping;
        }
        if (other.canCreateRootFolder != null) {
            canCreateRootFolder = other.canCreateRootFolder;
        }
        if (other.acls != null) {
            ACLDescriptor[] otherAcls = new ACLDescriptor[acls.length + other.acls.length];
            System.arraycopy(acls, 0, otherAcls, 0, acls.length);
            System.arraycopy(other.acls, 0, otherAcls, acls.length, other.acls.length);
            acls = otherAcls;
        }
    }

    @Override
    public CoreDirectory newDirectory() {
        return new CoreDirectory(this);
    }

}
