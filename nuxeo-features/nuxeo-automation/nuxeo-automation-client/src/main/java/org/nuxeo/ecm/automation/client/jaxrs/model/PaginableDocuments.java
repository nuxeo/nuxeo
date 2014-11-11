/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
