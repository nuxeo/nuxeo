/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.functionaltests.forms;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Base class to handle widgets
 * <p>
 * Needs a constructor accepting {@link WebDriver} and {@link String} as id to be instantiated by the
 * {@link LayoutElement#getWidget(String, Class)} method.
 *
 * @since 5.7
 */
public abstract class AbstractWidgetElement extends LayoutElement {

    public AbstractWidgetElement(WebDriver driver, String id) {
        super(driver, id);
    }

    public String getWidgetId() {
        String res = id;
        if (res.contains(":")) {
            res = res.substring(res.lastIndexOf(":") + 1);
        }
        return res;
    }

    /**
     * Returns the message element value, e.g. errors for this widget.
     *
     * @since 7.2
     */
    public String getMessageValue() {
        return getMessageValue("_message");
    }

    /**
     * Returns the message element value, e.g. errors for this widget.
     *
     * @since 7.2
     */
    public String getMessageValue(String suffix) {
        WebElement el = getElement(id + suffix);
        if (el != null) {
            return el.getText();
        }
        return null;
    }

}
