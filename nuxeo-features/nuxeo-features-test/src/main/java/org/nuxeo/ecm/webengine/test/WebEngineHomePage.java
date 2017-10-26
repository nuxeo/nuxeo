/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.test;

import org.nuxeo.runtime.test.runner.web.WebPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @deprecated since 9.3
 */
@Deprecated
public class WebEngineHomePage extends WebPage {

    public LoginPage getLoginPage() {
        return getPage(LoginPage.class);
    }

    protected WebElement getModuleLink(String name) {
        return findElement(By.partialLinkText(name));
    }

    public boolean hasModule(String name) {
        return hasElement(By.partialLinkText(name));
    }

    public <T extends WebPage> T getModulePage(String name, Class<T> pageClass) {
        WebElement link = getModuleLink(name);
        link.click();
        return getPage(pageClass);
    }
}
