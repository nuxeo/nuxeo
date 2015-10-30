/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nelson Silva <nsilva@nuxeo.com>
 */
package org.nuxeo.ftest.cap;

import org.apache.commons.lang.time.DateUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.EventListener;
import org.nuxeo.functionaltests.pages.admincenter.activity.RepositoryAnalyticsPage;
import org.nuxeo.functionaltests.pages.admincenter.activity.SearchAnalyticsPage;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

import java.util.Date;

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
