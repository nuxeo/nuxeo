/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */

package org.nuxeo.functionaltests.pages.wizard;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.functionaltests.AbstractTest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class WizardPage extends AbstractWizardPage {

    protected static final String NEXT_BUTTON_LOCATOR = "id('btnNext')";

    protected static final String PREV_BUTTON_LOCATOR = "id('btnPrev')";

    public WizardPage(WebDriver driver) {
        super(driver);
        IFrameHelper.focusOnWizardPage(driver);
    }

    public WizardPage next() {
        return next(false);
    }

    public WizardPage next(Boolean errorExpected) {
        if (errorExpected) {
            return next(WizardPage.class, driver -> hasError());
        } else {
            return next(WizardPage.class);
        }
    }

    public WizardPage previous() {
        return previous(false);
    }

    public WizardPage previous(Boolean errorExpected) {
        if (errorExpected) {
            return previous(WizardPage.class, driver -> hasError());
        } else {
            return previous(WizardPage.class);
        }
    }

    @Override
    protected By getNextButtonLocator() {
        return By.xpath(NEXT_BUTTON_LOCATOR);
    }

    @Override
    protected By getPreviousButtonLocator() {
        return By.xpath(PREV_BUTTON_LOCATOR);
    }

    public ConnectWizardPage getConnectPage() {
        AbstractTest.switchToPopup("/register/#/embedded");
        return AbstractTest.asPage(ConnectWizardPage.class);
    }

    public boolean hasError() {
        return getErrors().size() > 0;
    }

    public List<String> getErrors() {
        List<WebElement> errorsEl = driver.findElements(By.xpath("//div[@class='errBlock']/div[@class='errItem']"));
        List<String> errors = new ArrayList<String>();
        for (WebElement errorEl : errorsEl) {
            if (errorEl.getText() != null && errorEl.getText().length() > 0) {
                errors.add(errorEl.getText());
            }
        }
        return errors;
    }

    public List<String> getInfos() {
        List<WebElement> infosEl = driver.findElements(By.xpath("//div[@class='infoBlock']/div[@class='infoItem']"));
        List<String> infos = new ArrayList<String>();
        for (WebElement infoEl : infosEl) {
            if (infoEl.getText() != null && infoEl.getText().length() > 0) {
                infos.add(infoEl.getText());
            }
        }
        return infos;
    }

    public boolean hasInfo() {
        return getInfos().size() > 0;
    }

    public String getTagText(String tag) {
        WebElement tagElt = findElementWithTimeout(By.xpath("//" + tag));
        if (tagElt == null) {
            return null;
        }
        return tagElt.getText();
    }

}
