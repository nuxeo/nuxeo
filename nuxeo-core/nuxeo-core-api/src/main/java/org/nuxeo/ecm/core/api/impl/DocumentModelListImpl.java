/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.api.impl;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;

/**
 * @author Bogdan Stefanescu
 * @author Florent Guillaume
 */
public class DocumentModelListImpl extends ArrayList<DocumentModel> implements
        DocumentModelList {

    private static final long serialVersionUID = 4214422534444037559L;

    protected long totalSize = -1;

    public DocumentModelListImpl() {
    }

    public DocumentModelListImpl(int size) {
        super(size);
    }

    public DocumentModelListImpl(List<DocumentModel> list) {
        super(list);
    }

    /**
     * Constructs a DocumentModelListImpl and sets the "total size" information.
     * <p>
     * The total size is additional information that can be provided in some
     * cases where the list returned is a slice of a bigger list, this is used
     * when getting paged results from a database for instance.
     *
     * @param list the list of documents
     * @param totalSize the total size, with -1 meaning
     *            "same as the list's size"
     */
    public DocumentModelListImpl(List<DocumentModel> list, long totalSize) {
        super(list);
        this.totalSize = totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public long totalSize() {
        if (totalSize == -1) {
            return size();
        }
        return totalSize;
    }

}
