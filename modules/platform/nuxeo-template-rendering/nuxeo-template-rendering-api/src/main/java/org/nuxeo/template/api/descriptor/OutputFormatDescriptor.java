/*
 * (C) Copyright 2006-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     ldoguin
 */
package org.nuxeo.template.api.descriptor;

import static org.apache.commons.lang3.ObjectUtils.getIfNull;
import static org.apache.commons.lang3.StringUtils.defaultIfBlank;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

@XObject("outputFormat")
public class OutputFormatDescriptor implements Descriptor {

    @XNode("@id")
    protected String id;

    @XNode("@label")
    protected String label;

    @XNode("@enabled")
    protected boolean enabled = true;

    @XNode("@chainId")
    protected String chainId;

    @XNode("@mimetype")
    protected String mimeType;

    @Override
    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getChainId() {
        return chainId;
    }

    public String getMimeType() {
        return mimeType;
    }

    @Override
    public Descriptor merge(Descriptor o) {
        var other = (OutputFormatDescriptor) o;
        var merged = new OutputFormatDescriptor();
        merged.id = getIfNull(other.id, id);
        merged.label = defaultIfBlank(other.label, label);
        merged.enabled = other.enabled;
        merged.chainId = defaultIfBlank(other.chainId, chainId);
        merged.mimeType = defaultIfBlank(other.mimeType, mimeType);
        return merged;
    }
}
