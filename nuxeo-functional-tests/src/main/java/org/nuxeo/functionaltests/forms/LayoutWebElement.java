/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.functionaltests.forms;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.Map;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.testng.Assert;

/**
 * Represents a layout on the page, with helper methods to retrieve its
 * widgets.
 *
 * @since 5.7
 */
public class LayoutWebElement extends AbstractPage {

    protected String id;

    public LayoutWebElement(WebDriver driver, String id) {
        super(driver);
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * Returns a sub element, concatenating the layout id with the sub element
     * id.
     */
    public String getSubElementId(String id) {
        String finalId = id;
        if (this.id != null) {
            finalId = this.id + ":" + id;
        }
        return finalId;
    }

    protected <T> T instantiateWidget(String id, Class<T> pageClassToProxy) {
        try {
            try {
                Constructor<T> constructor = pageClassToProxy.getConstructor(
                        WebDriver.class, String.class);
                return constructor.newInstance(driver, getSubElementId(id));
            } catch (NoSuchMethodException e) {
                return pageClassToProxy.newInstance();
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a {@link WidgetElement} instantiated thanks to the driver and
     * given id, after having injected its fields.
     *
     * @since 5.7
     */
    public <T> T getWidget(String id, Class<T> widgetClassToProxy) {
        T res = instantiateWidget(id, widgetClassToProxy);
        res = AbstractTest.fillElement(widgetClassToProxy, res);
        return res;
    }

    protected boolean hasSubElement(String id) {
        return hasElement(By.id(getSubElementId(id)));
    }

    protected WebElement getSubElement(String id) {
        return driver.findElement(By.id(getSubElementId(id)));
    }

    public void fillForm(String id, String value) {
        WebElement elt = getSubElement(id);
        if (value == null) {
            elt.sendKeys("");
        } else {
            elt.sendKeys(value);
        }
    }

    public void fillForm(Map<String, String> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            fillForm(entry.getKey(), entry.getValue());
        }
    }

    public void checkForm(Map<String, Serializable> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Serializable> entry : entries.entrySet()) {
            WebElement elt = getSubElement(entry.getKey());
            if (elt != null) {
                String text = elt.getText();
                Serializable value = entry.getValue();
                Assert.assertEquals(text, value);
            }
        }
    }

}
