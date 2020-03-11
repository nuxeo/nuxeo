/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.query.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Provider simulating mock results given a page size ands a total number of results
 *
 * @author Anahide Tchertchian
 */
public class MockPageProvider extends AbstractPageProvider<MockPagedListItem> {

    private static final long serialVersionUID = 1L;

    protected List<MockPagedListItem> currentItems;

    protected final long givenResultsCount;

    protected final boolean knowsResultsCount;

    public MockPageProvider() {
        super();
        givenResultsCount = 0;
        knowsResultsCount = false;
    }

    public MockPageProvider(long pageSize, long resultsCount, boolean knowsResultsCount) {
        setPageSize(pageSize);
        givenResultsCount = resultsCount;
        this.knowsResultsCount = knowsResultsCount;
        if (knowsResultsCount) {
            setResultsCount(resultsCount);
        } else {
            setResultsCount(UNKNOWN_SIZE_AFTER_QUERY);
        }
    }

    @Override
    public List<MockPagedListItem> getCurrentPage() {
        long usedPageSize = givenResultsCount;
        long pageSize = getPageSize();
        if (pageSize > 0) {
            usedPageSize = pageSize;
        }
        currentItems = new ArrayList<>();
        long offset = getCurrentPageOffset();
        for (long i = offset; i < offset + usedPageSize && i < givenResultsCount; i++) {
            currentItems.add(getItem(i));
        }
        if (!knowsResultsCount) {
            // additional info to handle next page when results count is
            // unknown
            if (currentItems != null && currentItems.size() > 0) {
                int higherNonEmptyPage = getCurrentHigherNonEmptyPageIndex();
                int currentFilledPage = Long.valueOf(getCurrentPageIndex()).intValue();
                if (currentFilledPage > higherNonEmptyPage) {
                    setCurrentHigherNonEmptyPageIndex(currentFilledPage);
                }
            }
        }
        return currentItems;
    }

    protected MockPagedListItem getItem(long position) {
        return new MockPagedListItem(String.format("Mock_%s", Long.valueOf(position)), position);
    }

    @Override
    protected void pageChanged() {
        currentItems = null;
        super.pageChanged();
    }

    @Override
    public void refresh() {
        currentItems = null;
        setResultsCount(givenResultsCount);
        super.refresh();
    }

}
