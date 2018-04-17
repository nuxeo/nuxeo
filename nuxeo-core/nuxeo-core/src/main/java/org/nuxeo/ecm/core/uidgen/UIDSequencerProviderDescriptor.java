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

/**
 * @since 7.3
 */
@XObject("sequencer")
public class UIDSequencerProviderDescriptor {

    @XNode("@name")
    protected String name;

    @XNode("@default")
    protected boolean isdefault;

    @XNode("@class")
    protected Class<? extends UIDSequencer> sequencerClass;

    @XNode("@enabled")
    protected boolean enabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public UIDSequencer getSequencer() throws Exception {

        if (sequencerClass != null) {
            return sequencerClass.newInstance();
        }

        return null;
    }

    public String getName() {
        if (name == null && sequencerClass != null) {
            name = sequencerClass.getSimpleName();
        }
        return name;
    }

    public boolean isIsdefault() {
        return isdefault;
    }

}
