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
package org.nuxeo.ecm.core.api;

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

    protected long givenResultsCount;

    public MockPageProvider(long pageSize, long resultsCount) {
        this.pageSize = pageSize;
        givenResultsCount = resultsCount;
        this.resultsCount = resultsCount;
    }

    @Override
    public List<MockPagedListItem> getCurrentPage() {
        long usedPageSize = givenResultsCount;
        if (pageSize > 0) {
            usedPageSize = pageSize;
        }
        currentItems = new ArrayList<MockPagedListItem>();
        long offset = getCurrentPageOffset();
        for (long i = offset; i < offset + usedPageSize && i < resultsCount; i++) {
            currentItems.add(getItem(i));
        }
        return currentItems;
    }

    protected MockPagedListItem getItem(long position) {
        return new MockPagedListItem(String.format("Mock_%s",
                Long.valueOf(position)), position);
    }

    @Override
    public void refresh() {
        super.refresh();
        currentItems = null;
        resultsCount = givenResultsCount;
    }

}
