/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     ataillefer
 */
package org.nuxeo.ecm.diff.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Diff display descriptor.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
@XObject("diffDisplay")
public class DiffDisplayDescriptor {

    @XNode("@type")
    public String type;

    @XNode("@enabled")
    public boolean enabled = true;

    @XNodeList(value = "diffBlocks/diffBlock", type = ArrayList.class, componentType = DiffBlockReferenceDescriptor.class)
    public List<DiffBlockReferenceDescriptor> diffBlocks = new ArrayList<DiffBlockReferenceDescriptor>();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<DiffBlockReferenceDescriptor> getDiffBlocks() {
        return diffBlocks;
    }

    public void setDiffBlocks(List<DiffBlockReferenceDescriptor> diffBlocks) {
        this.diffBlocks = diffBlocks;
    }
}
