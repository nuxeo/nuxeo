/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id$
 */

package org.nuxeo.ecm.platform.audit.ws;

import java.io.Serializable;

public class ModifiedDocumentDescriptorPage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int pageIndex;

    private final boolean bHasMorePage;

    private final ModifiedDocumentDescriptor[] modifiedDocuments;

    public ModifiedDocumentDescriptorPage() {
        this(null, 0, false);
    }

    public ModifiedDocumentDescriptorPage(ModifiedDocumentDescriptor[] modifiedDocuments,
            int pageIndex, boolean bHasMorePage) {
        this.pageIndex = pageIndex;
        this.bHasMorePage = bHasMorePage;
        this.modifiedDocuments = modifiedDocuments;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public boolean hasMorePage() {
        return bHasMorePage;
    }

    public ModifiedDocumentDescriptor[] getModifiedDocuments() {
        return modifiedDocuments;
    }

    public boolean getHasMorePage() {
        return bHasMorePage;
    }

}
