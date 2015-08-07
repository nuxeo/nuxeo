/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: EventDescriptor.java 19481 2007-05-27 10:50:10Z sfermigier $
 */

package org.nuxeo.ecm.platform.audit.service.extension;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Really simple auditable event descriptor.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@XObject("event")
public class EventDescriptor {

    @XNode("@name")
    private String name;

    @XNode("@enabled")
    private boolean enabled = true;

    @XNodeList(value = "extendedInfos/extendedInfo", type = ArrayList.class, componentType = ExtendedInfoDescriptor.class)
    protected List<ExtendedInfoDescriptor> extendedInfoDescriptors;

    public boolean getEnabled() {
        return enabled;
    }

    /**
     * @since 7.4
     */
    public List<ExtendedInfoDescriptor> getExtendedInfoDescriptors() {
        return extendedInfoDescriptors;
    }

    public String getName() {
        return name;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * @since 7.4
     */
    public void setExtendedInfoDescriptors(List<ExtendedInfoDescriptor> extendedInfoDescriptors) {
        this.extendedInfoDescriptors = extendedInfoDescriptors;
    }

    public void setName(String name) {
        this.name = name;
    }

}
