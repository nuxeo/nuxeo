/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.api.impl;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.DocumentRefList;

/**
 * @author Bogdan Stefanescu
 */
public class DocumentRefListImpl extends ArrayList<DocumentRef> implements DocumentRefList {

    private static final long serialVersionUID = -7915146644486566862L;

    protected long totalSize = -1;

    public DocumentRefListImpl() {
    }

    public DocumentRefListImpl(int size) {
        super(size);
    }

    public DocumentRefListImpl(List<DocumentRef> list) {
        super(list);
    }

    /**
     * Constructs a DocumentModelListImpl and sets the "total size" information.
     * <p>
     * The total size is additional information that can be provided in some cases where the list returned is a slice of
     * a bigger list, this is used when getting paged results from a database for instance.
     *
     * @param list the list of documents
     * @param totalSize the total size, with -1 meaning "same as the list's size"
     */
    public DocumentRefListImpl(List<DocumentRef> list, long totalSize) {
        super(list);
        this.totalSize = totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    @Override
    public long totalSize() {
        if (totalSize == -1) {
            return size();
        }
        return totalSize;
    }

}
