/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 *
 * $Id: MultiDirectoryDescriptor.java 24597 2007-09-05 16:04:04Z fguillaume $
 */

package org.nuxeo.ecm.directory.multi;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.directory.PermissionDescriptor;

/**
 * @author Florent Guillaume
 */
@XObject(value = "directory")
public class MultiDirectoryDescriptor implements Cloneable {

    @XNode("@name")
    public String name;

    @XNode("schema")
    protected String schemaName;

    @XNode("idField")
    protected String idField;

    @XNode("passwordField")
    protected String passwordField;

    @XNode("readOnly")
    public Boolean readOnly;

    @XNode("querySizeLimit")
    public Integer querySizeLimit;

    @XNode("@remove")
    public boolean remove = false;

    @XNodeList(value = "source", type = SourceDescriptor[].class, componentType = SourceDescriptor.class)
    protected SourceDescriptor[] sources;

    @XNodeList(value = "permissions/permission", type = PermissionDescriptor[].class, componentType = PermissionDescriptor.class)
    public PermissionDescriptor[] permissions = null;

    public void merge(MultiDirectoryDescriptor other) {
        merge(other, false);
    }

    public void merge(MultiDirectoryDescriptor other, boolean overwrite) {
        if (other.schemaName != null || overwrite) {
            schemaName = other.schemaName;
        }
        if (other.idField != null || overwrite) {
            idField = other.idField;
        }
        if (other.passwordField != null || overwrite) {
            passwordField = other.passwordField;
        }
        if (other.readOnly != null || overwrite) {
            readOnly = other.readOnly;
        }
        if (other.querySizeLimit != null || overwrite) {
            querySizeLimit = other.querySizeLimit;
        }
        if (other.sources != null || overwrite) {
            if (sources == null) {
                sources = other.sources;
            } else {
                SourceDescriptor[] s = new SourceDescriptor[sources.length + other.sources.length];
                System.arraycopy(sources, 0, s, 0, sources.length);
                System.arraycopy(other.sources, 0, s, sources.length, other.sources.length);
                sources = s;
            }
        }
        if ((other.permissions != null && other.permissions.length != 0) || overwrite) {
            permissions = other.permissions;
        }
    }

    /**
     * @since 5.6
     */
    @Override
    public MultiDirectoryDescriptor clone() {
        MultiDirectoryDescriptor clone = new MultiDirectoryDescriptor();
        clone.name = name;
        clone.schemaName = schemaName;
        clone.idField = idField;
        clone.passwordField = passwordField;
        clone.readOnly = readOnly;
        clone.querySizeLimit = querySizeLimit;
        clone.remove = remove;
        if (sources != null) {
            clone.sources = new SourceDescriptor[sources.length];
            for (int i = 0; i < sources.length; i++) {
                clone.sources[i] = sources[i].clone();
            }
        }
        if (permissions != null) {
            clone.permissions = new PermissionDescriptor[permissions.length];
            for (int i = 0; i < permissions.length; i++) {
                clone.permissions[i] = permissions[i].clone();
            }
        }
        return clone;
    }

}
