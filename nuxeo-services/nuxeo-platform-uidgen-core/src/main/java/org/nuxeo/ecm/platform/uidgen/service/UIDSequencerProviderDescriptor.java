/*
 * (C) Copyright 2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thierry Delprat
 *
 */

package org.nuxeo.ecm.platform.uidgen.service;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.uidgen.UIDSequencer;

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
