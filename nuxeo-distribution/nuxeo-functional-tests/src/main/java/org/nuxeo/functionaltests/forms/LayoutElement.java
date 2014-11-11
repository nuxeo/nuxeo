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

import java.lang.reflect.Constructor;
import java.util.Map;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Represents a layout on the page, with helper methods to retrieve its
 * widgets.
 *
 * @since 5.7
 */
public class LayoutElement extends AbstractPage {

    protected String id;

    public LayoutElement(WebDriver driver, String id) {
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
     * id (and using the standard charcater ':' as JSF UINamingContainer
     * separator).
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
     * Returns a {@link WidgetElement} with given id, after having injected its
     * fields.
     */
    public WidgetElement getWidget(String id) {
        return getWidget(id, WidgetElement.class);
    }

    /**
     * Returns a widget with given id , after having injected its fields.
     */
    public <T> T getWidget(String id, Class<T> widgetClassToProxy) {
        T res = instantiateWidget(id, widgetClassToProxy);
        res = AbstractTest.fillElement(widgetClassToProxy, res);
        return res;
    }

    /**
     * Returns true if sub element is found in the page.
     */
    protected boolean hasSubElement(String id) {
        return hasElement(By.id(getSubElementId(id)));
    }

    /**
     * Returns the element with given id in the page.
     */
    public WebElement getElement(String id) {
        return driver.findElement(By.id(id));
    }

    /**
     * Returns the element with given id in the page.
     *
     * @param wait if true, waits for a default timeout (useful when element is
     *            added to the page after an ajax call).
     */
    public WebElement getElement(String id, boolean wait) {
        return AbstractTest.findElementWithTimeout(By.id(id));
    }

    /**
     * Returns the element with given sub id on the page.
     * <p>
     * The layout id is concatenated to the sub element id for retrieval.
     */
    public WebElement getSubElement(String id) {
        return getElement(getSubElementId(id));
    }

    /**
     * Returns the element with given sub id on the page.
     * <p>
     * The layout id is concatenated to the sub element id for retrieval.
     *
     * @param wait if true, waits for a default timeout (useful when element is
     *            added to the page after an ajax call).
     */
    public WebElement getSubElement(String id, boolean wait) {
        return getElement(getSubElementId(id), wait);
    }

    /**
     * Clears the given input element and sets the given value if not null.
     */
    public void setInput(WebElement elt, String value) {
        elt.clear();
        if (value != null) {
            elt.sendKeys(value);
        }
    }

    /**
     * Retrieves sub input element with given id and sets the given value.
     *
     * @see #setInput(WebElement, String)
     */
    public void setInput(String id, String value) {
        WebElement elt = getSubElement(id);
        setInput(elt, value);
    }

    /**
     * Retrieves sub input elements with given ids and sets corresponding
     * values.
     *
     * @see #setInput(String, String)
     */
    public void setInput(Map<String, String> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            setInput(entry.getKey(), entry.getValue());
        }
    }

}
