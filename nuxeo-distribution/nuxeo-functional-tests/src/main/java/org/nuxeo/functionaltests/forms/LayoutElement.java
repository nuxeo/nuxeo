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
import org.nuxeo.functionaltests.Assert;
import org.nuxeo.functionaltests.Locator;
import org.nuxeo.functionaltests.fragment.WebFragment;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

/**
 * Represents a layout on the page, with helper methods to retrieve its widgets.
 *
 * @since 5.7
 */
public class LayoutElement implements LayoutFragment {

    protected final WebDriver driver;

    protected String id;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    /**
     * @param driver
     * @param id
     */
    public LayoutElement(WebDriver driver, String id) {
        this.driver = driver;
        this.id = id;
    }

    /**
     * Returns a sub element, concatenating the layout id with the sub element id (and using the standard character ':'
     * as JSF UINamingContainer separator).
     */
    @Override
    public String getSubElementId(String id) {
        String finalId = id;
        if (this.id != null) {
            if (this.id.endsWith(":")) {
                finalId = this.id + id;
            } else {
                finalId = this.id + ":" + id;
            }
        }
        return finalId;
    }

    protected <T> T instantiateWidget(String id, Class<T> pageClassToProxy) {
        try {
            try {
                Constructor<T> constructor = pageClassToProxy.getConstructor(WebDriver.class, String.class);
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
     * Returns a {@link WidgetElement} with given id, after having injected its fields.
     */
    @Override
    public WidgetElement getWidget(String id) {
        return getWidget(id, WidgetElement.class);
    }

    /**
     * Returns a widget with given id , after having injected its fields.
     */
    @Override
    public <T> T getWidget(String id, Class<T> widgetClassToProxy) {
        T res = instantiateWidget(id, widgetClassToProxy);
        res = AbstractTest.fillElement(widgetClassToProxy, res);
        return res;
    }

    /**
     * Returns true if sub element is found in the page.
     */
    protected boolean hasSubElement(String id) {
        return Assert.hasElement(By.id(getSubElementId(id)));
    }

    /**
     * Returns the element with given id in the page.
     */
    public WebElement getElement(String id) {
        return Locator.findElement(By.id(id));
    }

    /**
     * Returns the element with given id in the page.
     *
     * @param wait if true, waits for a default timeout (useful when element is added to the page after an ajax call).
     */
    public WebElement getElement(String id, boolean wait) {
        return Locator.findElementWithTimeout(By.id(id));
    }

    /**
     * Returns the element with given sub id on the page.
     * <p>
     * The layout id is concatenated to the sub element id for retrieval.
     */
    @Override
    public WebElement getSubElement(String id) {
        return getElement(getSubElementId(id));
    }

    /**
     * Returns the element with given sub id on the page.
     * <p>
     * The layout id is concatenated to the sub element id for retrieval.
     *
     * @param wait if true, waits for a default timeout (useful when element is added to the page after an ajax call).
     */
    @Override
    public WebElement getSubElement(String id, boolean wait) {
        return getElement(getSubElementId(id), wait);
    }

    /**
     * Clears the given input element and sets the given value if not null.
     */
    public void setInput(WebElement elt, String value) {
        elt.click();
        if (value != null) {
            elt.sendKeys(
                    // replace existing input content when typing, as the clear() method crashes on boolean elements for
                    // instance
                    Keys.chord(Keys.CONTROL, "a"), // select input content for linux
                    Keys.chord(Keys.COMMAND, "a"), // select input content for macos
                    value); // add new value
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
     * Retrieves sub input elements with given ids and sets corresponding values.
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

    /**
     * @since 5.9.2
     */
    public <T extends WebFragment> T getWebFragment(String id, Class<T> webFragmentClass) {
        return AbstractTest.getWebFragment(By.id(getSubElementId(id)), webFragmentClass);
    }

}
