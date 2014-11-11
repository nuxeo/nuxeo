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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.core.api;

import java.io.Serializable;
import java.util.List;

/**
 * A list of DocumentModels representing a chunk of a larger result, usually
 * retrieved as an iterator. The role of this chunk is to group a small piece
 * of the entire result to be fetched at once.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class DocumentModelsChunk implements Serializable {

    private static final long serialVersionUID = -8988171194412097562L;

    /**
     * Last index of non-filtered elements list. This is the offset (in the
     * query/initial result) from where the next chunk will be constructed.
     */
    public final int lastIndex;

    /**
     * Tells if there are more results to come after the current chunk.
     */
    public final boolean hasMore;

    private final List<DocumentModel> list;

    private final long max;

    /**
     * @param the max maximum number of items in the result. This number could be at most
     *   the total number of Documents before filtering.
     */
    DocumentModelsChunk(DocumentModelList list, int lastIndex, boolean hasMore, long max) {
        this.list = list;
        this.lastIndex = lastIndex;
        this.hasMore = hasMore;
        this.max = max;
    }

    /**
     * @return the number of elements from this chunk
     */
    public int getSize() {
        return list.size();
    }

    /**
     * @return the element at position i
     */
    public DocumentModel getItem(int i) {
        return list.get(i);
    }

    public void remove(int pos) {
        list.remove(pos);
    }

    /**
     * @return the maximum number of items that could be retrieved from the source
     */
    public long getMax() {
        return max;
    }

}
