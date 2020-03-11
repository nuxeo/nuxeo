/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.core.versioning;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.VersioningOption;

/**
 * Descriptor to contribute new restrictions in versioning system.
 * <p />
 * These contributions will restrict available increment options on UI and raise an exception when saving a
 * {@link DocumentModel} with an excluded {@link VersioningOption}.
 *
 * @since 9.1
 */
@XObject("restriction")
public class VersioningRestrictionDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@type")
    protected String type;

    @XNodeMap(value = "options", key = "@lifeCycleState", type = HashMap.class, componentType = VersioningRestrictionOptionsDescriptor.class)
    protected Map<String, VersioningRestrictionOptionsDescriptor> options = new HashMap<>();

    public String getType() {
        return type;
    }

    public VersioningRestrictionOptionsDescriptor getRestrictionOption(String key) {
        return options.get(key);
    }

    public void merge(VersioningRestrictionDescriptor other) {
        if (other.type != null) {
            type = other.type;
        }
        options.putAll(other.options); // always merge options
    }

}
