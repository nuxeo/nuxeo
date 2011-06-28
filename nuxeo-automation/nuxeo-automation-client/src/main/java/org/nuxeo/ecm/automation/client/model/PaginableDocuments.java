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
package org.nuxeo.ecm.automation.client.model;

import java.util.List;



/**
 * @author matic
 *
 */
public class PaginableDocuments extends Documents {

    private static final long serialVersionUID = 1L;

    protected int totalSize;
    protected int pageSize;
    protected int pageCount;
    protected int pageIndex;

    public PaginableDocuments() {
    }

    /**
     * @param size
     */
    public PaginableDocuments(List<Document> docs, int totalSize, int pageSize, int pageCount, int pageIndex) {
        super (docs);
        this.totalSize = totalSize;
        this.pageSize = pageSize;
        this.pageCount = pageCount;
        this.pageIndex = pageIndex;
    }

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

    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

}
