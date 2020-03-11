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
 * $Id: SubDirectoryDescriptor.java 24597 2007-09-05 16:04:04Z fguillaume $
 */

package org.nuxeo.ecm.directory.multi;

import java.util.Arrays;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author Florent Guillaume
 */
@XObject("subDirectory")
public class SubDirectoryDescriptor implements Cloneable {

    @XNode("@name")
    public String name;

    @XNode("optional")
    public boolean isOptional = false;

    @XNodeList(value = "field", type = FieldDescriptor[].class, componentType = FieldDescriptor.class)
    public FieldDescriptor[] fields;

    @Override
    public String toString() {
        return String.format("{subdirectory name=%s fields=%s", name, Arrays.toString(fields));
    }

    /**
     * @since 5.6
     */
    @Override
    public SubDirectoryDescriptor clone() {
        SubDirectoryDescriptor clone;
        try {
            clone = (SubDirectoryDescriptor) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
        // basic fields are already copied by super.clone()
        if (fields != null) {
            clone.fields = new FieldDescriptor[fields.length];
            for (int i = 0; i < fields.length; i++) {
                clone.fields[i] = fields[i].clone();
            }
        }
        return clone;
    }
}
