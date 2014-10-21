/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
                SourceDescriptor[] s = new SourceDescriptor[sources.length
                        + other.sources.length];
                System.arraycopy(sources, 0, s, 0, sources.length);
                System.arraycopy(other.sources, 0, s, sources.length,
                        other.sources.length);
                sources = s;
            }
        }
        if ((other.permissions != null && other.permissions.length != 0)
                || overwrite) {
            permissions = other.permissions;
        }
    }

    /**
     * @since 5.6
     */
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
