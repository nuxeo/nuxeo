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
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.BaseDirectoryDescriptor;

/**
 * @author Florent Guillaume
 */
@XObject(value = "directory")
public class MultiDirectoryDescriptor extends BaseDirectoryDescriptor {

    @XNode("querySizeLimit")
    public Integer querySizeLimit;

    @XNodeList(value = "source", type = SourceDescriptor[].class, componentType = SourceDescriptor.class)
    protected SourceDescriptor[] sources;

    @Override
    public void merge(BaseDirectoryDescriptor other) {
        super.merge(other);
        if (other instanceof MultiDirectoryDescriptor) {
            merge((MultiDirectoryDescriptor) other);
        }
    }

    protected void merge(MultiDirectoryDescriptor other) {
        if (other.querySizeLimit != null) {
            querySizeLimit = other.querySizeLimit;
        }
        if (other.sources != null) {
            if (sources == null) {
                sources = other.sources;
            } else {
                SourceDescriptor[] s = new SourceDescriptor[sources.length + other.sources.length];
                System.arraycopy(sources, 0, s, 0, sources.length);
                System.arraycopy(other.sources, 0, s, sources.length, other.sources.length);
                sources = s;
            }
        }
    }

    /**
     * @since 5.6
     */
    @Override
    public MultiDirectoryDescriptor clone() {
        MultiDirectoryDescriptor clone = (MultiDirectoryDescriptor) super.clone();
        // basic fields are already copied by super.clone()
        if (sources != null) {
            clone.sources = new SourceDescriptor[sources.length];
            for (int i = 0; i < sources.length; i++) {
                clone.sources[i] = sources[i].clone();
            }
        }
        return clone;
    }

    @Override
    public Directory newDirectory() {
        return new MultiDirectory(this);
    }

}
