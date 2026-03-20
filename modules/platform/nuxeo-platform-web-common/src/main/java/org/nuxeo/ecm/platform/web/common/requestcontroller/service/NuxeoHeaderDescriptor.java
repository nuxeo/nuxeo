/*
 * (C) Copyright 2014-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thierry Martins
 */
package org.nuxeo.ecm.platform.web.common.requestcontroller.service;

import static org.apache.commons.lang3.ObjectUtils.getIfNull;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

/**
 * @author <a href="mailto:tm@nuxeo.com">Thierry Martins</a>
 * @since 6.0
 */
@XObject(value = "header")
public class NuxeoHeaderDescriptor implements Descriptor {

    @XNode("@name")
    protected String name;

    @XNode("@enabled")
    protected Boolean enabled = true;

    @XContent
    protected String value;

    @Override
    public String getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getValue() {
        return StringUtils.trim(value);
    }

    @Override
    public Descriptor merge(Descriptor o) {
        var other = (NuxeoHeaderDescriptor) o;
        var merged = new NuxeoHeaderDescriptor();
        merged.name = name; // we merge based on name, so no name merging needed
        merged.enabled = getIfNull(other.enabled, enabled);
        merged.value = getIfNull(other.value, value);
        return merged;
    }
}
