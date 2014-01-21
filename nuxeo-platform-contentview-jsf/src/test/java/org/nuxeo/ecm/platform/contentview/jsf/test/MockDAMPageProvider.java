/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.contentview.jsf.test;

import java.util.Arrays;
import java.util.List;

import org.nuxeo.ecm.platform.query.api.AbstractPageProvider;

/**
 * Mock class to check refresh state of a page provider.
 *
 * @since 5.9.2
 */
public class MockDAMPageProvider extends AbstractPageProvider<String> {

    private static final long serialVersionUID = 1L;

    protected int getCounter = 0;

    protected List<String> currentPage;

    @Override
    public List<String> getCurrentPage() {
        if (currentPage == null) {
            getCounter++;
            currentPage = Arrays.asList("foo", "bar");
        }
        return currentPage;
    }

    @Override
    protected void pageChanged() {
        currentPage = null;
        super.pageChanged();
    }

    @Override
    public void refresh() {
        currentPage = null;
        super.refresh();
    }

    public int getGetCounter() {
        return getCounter;
    }

    @Override
    protected void notifyRefresh() {
        // mock up DAM use case where refresh notification triggers a call to
        // getCurrentPage
        getCurrentPage();
        super.notifyRefresh();
    }

}
