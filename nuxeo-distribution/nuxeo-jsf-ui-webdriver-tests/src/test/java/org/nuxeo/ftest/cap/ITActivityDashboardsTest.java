/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nelson Silva <nsilva@nuxeo.com>
 */
package org.nuxeo.ftest.cap;

import java.util.Date;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.EventListener;
import org.nuxeo.functionaltests.pages.admincenter.activity.RepositoryAnalyticsPage;
import org.nuxeo.functionaltests.pages.admincenter.activity.SearchAnalyticsPage;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @since 7.10
 */
@RunWith(FeaturesRunner.class)
public class ITActivityDashboardsTest extends AbstractTest {

    private static final Date endDate = new Date();

    private static final Date startDate = DateUtils.addDays(endDate, -7);

    @Test
    public void testRepositoryAnalytics() throws Exception {
        RepositoryAnalyticsPage dashboard = login().getAdminCenter().getActivityPage().getRepositoryAnalyticsPage();
        EventListener listener = dashboard.listenForDataChanges();
        dashboard.setStartDate(startDate);
        // debouncing isn't working properly on FF so this could trigger more requests than expected
        // dashboard.setEndDate(endDate);
        listener.waitCalled(5);
        logout();
    }

    @Test
    public void testSearchAnalytics() throws Exception {
        SearchAnalyticsPage dashboard = login().getAdminCenter().getActivityPage().getSearchAnalyticsPage();
        EventListener listener = dashboard.listenForDataChanges();
        dashboard.setStartDate(startDate);
        // debouncing isn't working properly on FF so this could trigger more requests than expected
        // dashboard.setEndDate(endDate);
        listener.waitCalled(6);
        logout();
    }
}
