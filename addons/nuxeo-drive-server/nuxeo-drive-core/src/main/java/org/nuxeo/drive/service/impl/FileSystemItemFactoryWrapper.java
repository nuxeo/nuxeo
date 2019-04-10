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

import org.nuxeo.drive.service.FileSystemItemFactory;

/**
 * Wrapper for a {@link FileSystemItemFactory} instance.
 * 
 * @author Antoine Taillefer
 */
public class FileSystemItemFactoryWrapper implements Serializable {

    private static final long serialVersionUID = 6038185061388418020L;

    protected String docType;

    protected String facet;

    protected FileSystemItemFactory factory;

    public FileSystemItemFactoryWrapper(String docType, String facet, FileSystemItemFactory factory) {
        this.docType = docType;
        this.facet = facet;
        this.factory = factory;
    }

    public String getDocType() {
        return docType;
    }

    public String getFacet() {
        return facet;
    }

    public FileSystemItemFactory getFactory() {
        return factory;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(factory.getClass().getName());
        sb.append("(docType=");
        sb.append(getDocType());
        sb.append("/facet=");
        sb.append(getFacet());
        sb.append(")");
        return sb.toString();
    }

}
