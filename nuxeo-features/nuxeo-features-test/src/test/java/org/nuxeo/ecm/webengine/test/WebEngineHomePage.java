/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.test;

import org.nuxeo.runtime.test.runner.web.WebPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
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
