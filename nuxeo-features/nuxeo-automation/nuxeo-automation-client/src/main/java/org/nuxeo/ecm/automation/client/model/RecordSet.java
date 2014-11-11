/*
 * Copyright (c) 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo
 */

package org.nuxeo.ecm.automation.client.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Map;

/**
 * RecordSet object returned by QueryAndFetch
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * @since 5.7
 */
public class RecordSet extends ArrayList<Map<String, Serializable>> {

    private static final long serialVersionUID = 1L;

    protected int pageSize = -1;

    protected int pageCount = -1;

    protected int pageIndex = -1;

    public RecordSet() {
        super();
    }

    public RecordSet(int pageIndex, int pageSize, int pageCount) {
        super();
        this.pageIndex = pageIndex;
        this.pageSize = pageSize;
        this.pageCount = pageCount;
    }

    public boolean isPaginable() {
        return pageIndex >= 0;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public void setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
    }

}
