/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.audit.ws;

import java.io.Serializable;

public class ModifiedDocumentDescriptorPage implements Serializable {

    private static final long serialVersionUID = 1L;

    private int pageIndex;

    private boolean bHasMorePage;

    private ModifiedDocumentDescriptor[] modifiedDocuments;

    public ModifiedDocumentDescriptorPage() {
        this(null, 0, false);
    }

    public ModifiedDocumentDescriptorPage(ModifiedDocumentDescriptor[] modifiedDocuments, int pageIndex,
            boolean bHasMorePage) {
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

    public boolean isBHasMorePage() {
        return bHasMorePage;
    }

    public void setBHasMorePage(boolean hasMorePage) {
        bHasMorePage = hasMorePage;
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }

    public void setModifiedDocuments(ModifiedDocumentDescriptor[] modifiedDocuments) {
        this.modifiedDocuments = modifiedDocuments;
    }

}
