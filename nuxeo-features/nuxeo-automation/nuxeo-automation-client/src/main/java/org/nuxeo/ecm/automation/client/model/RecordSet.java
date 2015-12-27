/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
