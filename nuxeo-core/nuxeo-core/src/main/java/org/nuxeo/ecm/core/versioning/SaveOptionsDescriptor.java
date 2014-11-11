/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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
 * @since 5.4.1
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
        List<VersioningOption> opts = new LinkedList<VersioningOption>();
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
