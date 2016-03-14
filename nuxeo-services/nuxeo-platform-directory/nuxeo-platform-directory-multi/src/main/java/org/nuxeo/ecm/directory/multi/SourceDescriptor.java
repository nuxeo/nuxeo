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
 * $Id: SourceDescriptor.java 24597 2007-09-05 16:04:04Z fguillaume $
 */

package org.nuxeo.ecm.directory.multi;

import java.util.Arrays;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author Florent Guillaume
 */
@XObject("source")
public class SourceDescriptor implements Cloneable {

    @XNode("@name")
    public String name;

    @XNode("@creation")
    public boolean creation;

    @XNodeList(value = "subDirectory", type = SubDirectoryDescriptor[].class, componentType = SubDirectoryDescriptor.class)
    public SubDirectoryDescriptor[] subDirectories;

    @Override
    public String toString() {
        return String.format("{source name=%s subDirectories=%s", name, Arrays.toString(subDirectories));
    }

    /**
     * @since 5.6
     */
    @Override
    public SourceDescriptor clone() {
        SourceDescriptor clone;
        try {
            clone = (SourceDescriptor) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
        // basic fields are already copied by super.clone()
        if (subDirectories != null) {
            clone.subDirectories = new SubDirectoryDescriptor[subDirectories.length];
            for (int i = 0; i < subDirectories.length; i++) {
                clone.subDirectories[i] = subDirectories[i].clone();
            }
        }
        return clone;
    }

}
