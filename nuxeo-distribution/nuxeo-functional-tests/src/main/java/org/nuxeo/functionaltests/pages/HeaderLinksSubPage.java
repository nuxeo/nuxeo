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
 *     Benoit Delbosc
 *     Antoine Taillefer
 */
package org.nuxeo.functionaltests.pages;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class HeaderLinksSubPage extends AbstractPage {

    @FindBy(xpath = "//div[@class=\"userMenuActions\"]")
    public WebElement userActions;

    @FindBy(xpath = "//div[@class=\"userMenuActions\"]/ul/li/ul")
    public WebElement userActionLinks;

    @FindBy(xpath = "//div[@class=\"userMenuActions\"]/ul/li/ul/li/a[text()=\"Log out\"]")
    WebElement logoutLink;

    public HeaderLinksSubPage(WebDriver driver) {
        super(driver);
    }

    public String getText() {
        return userActions.getText();
    }

    public NavigationSubPage getNavigationSubPage() {
        return asPage(NavigationSubPage.class);
    }

}
