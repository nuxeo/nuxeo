/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.functionaltests.pages.admincenter.monitoring;

import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.admincenter.AdminCenterBasePage;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 5.9.6
 */
public class MonitoringPage extends AdminCenterBasePage {

    @Required
    @FindBy(linkText = "Logs")
    WebElement logsLink;

    public MonitoringPage(WebDriver driver) {
        super(driver);
    }

    public LogsPage getLogsPage() {
        logsLink.click();
        return asPage(LogsPage.class);
    }

}
