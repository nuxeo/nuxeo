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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.service.impl;

import java.io.Serializable;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.drive.service.FileSystemItemAdapterService;
import org.nuxeo.drive.service.FileSystemItemFactory;

/**
 * XMap descriptor for factories contributed to the
 * {@code fileSystemItemFactory} extension point of the
 * {@link FileSystemItemAdapterService}.
 *
 * @author Antoine Taillefer
 */
@XObject("fileSystemItemFactory")
public class FileSystemItemFactoryDescriptor implements Serializable,
        Comparable<FileSystemItemFactoryDescriptor> {

    private static final long serialVersionUID = -7840980495329452651L;

    @XNode("@name")
    protected String name;

    @XNode("@enabled")
    protected boolean enabled = true;

    @XNode("@order")
    protected int order;

    @XNode("@docType")
    protected String docType;

    @XNode("@facet")
    protected String facet;

    @XNode("@class")
    protected Class<? extends FileSystemItemFactory> factoryClass;

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getOrder() {
        return order;
    }

    public String getDocType() {
        return docType;
    }

    public String getFacet() {
        return facet;
    }

    public FileSystemItemFactory getFactory() throws InstantiationException,
            IllegalAccessException {
        return factoryClass.newInstance();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName());
        sb.append("(");
        sb.append(getOrder());
        sb.append(")");
        return sb.toString();
    }

    @Override
    public int compareTo(FileSystemItemFactoryDescriptor other) {
        if (other == null) {
            return 1;
        }
        return getOrder() - other.getOrder();
    }

}
