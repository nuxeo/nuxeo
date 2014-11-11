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
 *     dmetzler
 */
package org.nuxeo.functionaltests.pages.wizard;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 *
 *
 * @since 5.9.5
 */
public class ConnectRegistrationPage extends AbstractWizardPage{

    /**
     * @param driver
     */
    public ConnectRegistrationPage(WebDriver driver) {
        super(driver);
        IFrameHelper.focusOnConnectFrame(driver);
    }

    @Override
    public String getTitle() {
        WebElement title = findElementWithTimeout(By.xpath("//h1"), ((Long)TimeUnit.SECONDS.toMillis(20)).intValue());
        return title.getText().trim();
    }

    @Override
    protected String getNextButtonLocator() {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

    @Override
    protected String getPreviousButtonLocator() {
        // TODO Auto-generated method stub
        // return null;
        throw new UnsupportedOperationException();
    }

}
