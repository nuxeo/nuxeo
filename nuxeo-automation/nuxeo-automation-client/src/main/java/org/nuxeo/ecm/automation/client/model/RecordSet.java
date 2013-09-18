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

    protected int currentPageIndex = -1;

    protected int numberOfPages = -1;

    public RecordSet() {
        super();
    }

    public RecordSet(int currentPageIndex, int pageSize, int numberOfPages) {
        super();
        this.currentPageIndex = currentPageIndex;
        this.pageSize = pageSize;
        this.numberOfPages = numberOfPages;
    }

    public boolean isPaginable() {
        return currentPageIndex >= 0;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getCurrentPageIndex() {
        return currentPageIndex;
    }

    public void setCurrentPageIndex(int currentPageIndex) {
        this.currentPageIndex = currentPageIndex;
    }

    public int getNumberOfPages() {
        return numberOfPages;
    }

    public void setNumberOfPages(int numberOfPages) {
        this.numberOfPages = numberOfPages;
    }
}
