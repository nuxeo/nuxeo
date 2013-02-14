/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.query.api;

import java.util.ArrayList;
import java.util.List;

/**
 * Provider simulating mock results given a page size ands a total number of
 * results
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

    public MockPageProvider(long pageSize, long resultsCount,
            boolean knowsResultsCount) {
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
        currentItems = new ArrayList<MockPagedListItem>();
        long offset = getCurrentPageOffset();
        for (long i = offset; i < offset + usedPageSize
                && i < givenResultsCount; i++) {
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
        return new MockPagedListItem(String.format("Mock_%s",
                Long.valueOf(position)), position);
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
