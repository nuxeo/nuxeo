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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.service.impl;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.diff.model.DiffFieldItemDefinition;
import org.nuxeo.ecm.diff.model.impl.DiffFieldItemDefinitionImpl;

/**
 * Diff field item descriptor.
 *
 * @author Antoine Taillefer (ataillefer@nuxeo.com)
 * @since 5.6
 */
@XObject("item")
public class DiffFieldItemDescriptor {

    @XNode("@name")
    public String name;

    @XNode("@displayContentDiffLinks")
    public boolean displayContentDiffLinks;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDisplayContentDiffLinks() {
        return displayContentDiffLinks;
    }

    public void setDisplayContentDiffLinks(boolean displayContentDiffLinks) {
        this.displayContentDiffLinks = displayContentDiffLinks;
    }

    public DiffFieldItemDefinition getDiffFieldItemDefinition() {
        return new DiffFieldItemDefinitionImpl(getName(),
                isDisplayContentDiffLinks());
    }
}
