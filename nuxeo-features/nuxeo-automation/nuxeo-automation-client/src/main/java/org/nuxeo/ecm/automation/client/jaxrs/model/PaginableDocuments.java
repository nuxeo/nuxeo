/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     matic
 */
package org.nuxeo.ecm.automation.client.jaxrs.model;

/**
 * @author matic
 *
 */
public class PaginableDocuments extends Documents {

    /**
     * @param size
     */
    public PaginableDocuments(int size, int totalSize, int pageSize, int pageCount, int pageIndex) {
        super(size);
        this.totalSize = totalSize;
        this.pageSize = pageSize;
        this.pageCount = pageCount;
        this.pageIndex = pageIndex;
    }


    private static final long serialVersionUID = 1L;
    
    protected int totalSize;
    protected int pageSize;
    protected int pageCount;
    protected int pageIndex;
    
    public int getTotalSize() {
        return totalSize;
    }

    public int getPageSize() {
        return pageSize;
    }
    

    public int getPageCount() {
        return pageCount;
    }
    
    public int getPageIndex() {
        return pageIndex;
    }

}
