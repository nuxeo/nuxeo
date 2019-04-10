/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.drive.service.FileSystemItemAdapterService;

/**
 * XMap descriptor for the {@code activeFileSystemItemFactories} contributions to the
 * {@code activeFileSystemItemFactories} extension point of the {@link FileSystemItemAdapterService}.
 * 
 * @author Antoine Taillefer
 */
@XObject("activeFileSystemItemFactories")
public class ActiveFileSystemItemFactoriesDescriptor implements Serializable {

    private static final long serialVersionUID = -6359900042974173427L;

    @XNode("@merge")
    protected boolean merge = false;

    @XNodeList(value = "factories/factory", type = ArrayList.class, componentType = ActiveFileSystemItemFactoryDescriptor.class)
    protected List<ActiveFileSystemItemFactoryDescriptor> factories;

    public boolean isMerge() {
        return merge;
    }

    public void setMerge(boolean merge) {
        this.merge = merge;
    }

    public List<ActiveFileSystemItemFactoryDescriptor> getFactories() {
        return factories;
    }

    public void setFactories(List<ActiveFileSystemItemFactoryDescriptor> factories) {
        this.factories = factories;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("<merge = ");
        sb.append(merge);
        sb.append(", [");
        for (ActiveFileSystemItemFactoryDescriptor factory : factories) {
            sb.append(factory);
            sb.append(", ");
        }
        sb.append("]>");
        return sb.toString();
    }

}
