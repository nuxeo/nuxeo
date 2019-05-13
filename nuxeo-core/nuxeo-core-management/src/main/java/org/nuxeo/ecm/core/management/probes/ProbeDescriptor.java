/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     matic
 */
package org.nuxeo.ecm.core.management.probes;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.management.api.Probe;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
@XObject("probe")
public class ProbeDescriptor {

    @XNode("@name")
    private String shortcutName;

    @XNode("@qualifiedName")
    private String qualifiedName;

    @XNode("@class")
    private Class<? extends Probe> probeClass;

    @XNode("label")
    private String label;

    @XNode("description")
    private String description;

    public String getLabel() {
        if (label == null) {
            return "label." + shortcutName;
        }
        return label;
    }

    public String getDescription() {
        if (description == null) {
            return "description." + shortcutName;
        }
        return description;
    }

    public String getShortcut() {
        return shortcutName;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public Class<? extends Probe> getProbeClass() {
        return probeClass;
    }

}
