/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.ecm.core.uidgen;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.registry.XEnable;
import org.nuxeo.common.xmap.registry.XRegistry;
import org.nuxeo.common.xmap.registry.XRegistryId;

/**
 * @since 7.3
 */
@XObject("sequencer")
@XRegistry(enable = false)
public class UIDSequencerProviderDescriptor {

    @XNode(value = "@name", fallback = "@class")
    @XRegistryId
    protected String name;

    @XNode("@default")
    protected boolean isdefault;

    @XNode("@class")
    protected Class<? extends UIDSequencer> sequencerClass;

    @XNode(value = XEnable.ENABLE, fallback = "@enabled", defaultAssignment = "true")
    @XEnable
    protected boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public UIDSequencer getSequencer() throws ReflectiveOperationException {
        return sequencerClass.getDeclaredConstructor().newInstance();
    }

    public String getName() {
        return name;
    }

    public boolean isDefault() {
        return isdefault;
    }

}
