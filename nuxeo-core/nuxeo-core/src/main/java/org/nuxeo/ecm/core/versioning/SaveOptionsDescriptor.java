/*
 * (C) Copyright 2006-2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Laurent Doguin
 */
package org.nuxeo.ecm.core.versioning;

import static org.nuxeo.ecm.core.api.VersioningOption.MAJOR;
import static org.nuxeo.ecm.core.api.VersioningOption.MINOR;
import static org.nuxeo.ecm.core.api.VersioningOption.NONE;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.VersioningOption;

/**
 * Descriptor to contribute incrementation options.
 *
 * @author Laurent Doguin
 * @since 5.4.2
 */
@XObject("options")
public class SaveOptionsDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@lifeCycleState")
    private String lifeCycleState;

    @XNode("none")
    private OptionDescriptor none;

    @XNode("minor")
    private OptionDescriptor minor;

    @XNode("major")
    private OptionDescriptor major;

    public String getLifeCycleState() {
        return lifeCycleState;
    }

    public List<VersioningOption> getVersioningOptionList() {
        List<VersioningOption> opts = new LinkedList<>();
        if (none != null) {
            if (none.isDefault()) {
                opts.add(0, NONE);
            } else {
                opts.add(NONE);
            }
        }
        if (minor != null) {
            if (minor.isDefault()) {
                opts.add(0, MINOR);
            } else {
                opts.add(MINOR);
            }
        }
        if (major != null) {
            if (major.isDefault()) {
                opts.add(0, MAJOR);
            } else {
                opts.add(MAJOR);
            }
        }
        return opts;
    }
}
