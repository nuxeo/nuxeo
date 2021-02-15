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
 *     Nuxeo - initial API and implementation
 *
 */
package org.nuxeo.ecm.platform.mimetype.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;
import org.nuxeo.ecm.platform.mimetype.MimetypeEntryImpl;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;

/**
 * MimetypeEntry extension definition.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@XObject("mimetype")
@XRegistry(compatWarnOnMerge = true)
public class MimetypeDescriptor {

    @XNode("@normalized")
    @XRegistryId
    protected String normalized;

    @XNode("@binary")
    protected boolean binary = true;

    @XNode("@onlineEditable")
    protected boolean onlineEditable = false;

    @XNode("@oleSupported")
    protected boolean oleSupported = false;

    @XNode("@iconPath")
    protected String iconPath;

    @XNodeList(value = "mimetypes/mimetype", type = LinkedHashSet.class, componentType = String.class)
    protected Set<String> mimetypes;

    @XNodeList(value = "extensions/extension", type = LinkedHashSet.class, componentType = String.class)
    protected Set<String> extensions;

    public boolean isBinary() {
        return binary;
    }

    public boolean isOnlineEditable() {
        return onlineEditable;
    }

    public boolean isOleSupported() {
        return oleSupported;
    }

    public List<String> getExtensions() {
        return new ArrayList<>(extensions);
    }

    public String getIconPath() {
        return iconPath;
    }

    public List<String> getMimetypes() {
        return new ArrayList<>(mimetypes);
    }

    public MimetypeEntry getMimetype() {
        return new MimetypeEntryImpl(normalized, getMimetypes(), getExtensions(), iconPath, binary, onlineEditable,
                oleSupported);
    }

    public String getNormalized() {
        return normalized;
    }

}
