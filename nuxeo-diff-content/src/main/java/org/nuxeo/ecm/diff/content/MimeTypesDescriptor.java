/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.content;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor of the list of blacklisted mime types for HTML conversion.
 *
 * @since 10.10
 */
@XObject("mimeTypes")
public class MimeTypesDescriptor {

    @XNodeList(value = "mimeType", type = ArrayList.class, componentType = MimeTypeDescriptor.class)
    protected List<MimeTypeDescriptor> mimeTypes = new ArrayList<>();

    @XNode("@override")
    protected boolean override;

    public List<MimeTypeDescriptor> getMimeTypes() {
        return mimeTypes;
    }

    public void setMimeTypes(List<MimeTypeDescriptor> mimeTypes) {
        this.mimeTypes = mimeTypes;
    }

    public boolean isOverride() {
        return override;
    }

    public void setOverride(boolean override) {
        this.override = override;
    }

}
