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

/**
 * @author Florent Guillaume
 *
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

    public void merge(MultiDirectoryDescriptor other) {
        if (other.schemaName != null) {
            schemaName = other.schemaName;
        }
        if (other.idField != null) {
            idField = other.idField;
        }
        if (other.passwordField != null) {
            passwordField = other.passwordField;
        }
        if (other.readOnly != null) {
            readOnly = other.readOnly;
        }
        if (other.querySizeLimit != null) {
            querySizeLimit = other.querySizeLimit;
        }
        if (other.sources != null) {
            // TODO allow replacement of existing sources
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
    }

}
