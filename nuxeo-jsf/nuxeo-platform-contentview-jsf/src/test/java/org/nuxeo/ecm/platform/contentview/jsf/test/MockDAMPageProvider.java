/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
