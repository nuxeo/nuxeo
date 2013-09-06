/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.functionaltests.fragment;

import org.openqa.selenium.WebElement;

/**
 * @since 5.7.3
 */
public interface WebFragment extends WebElement {

    WebElement getElement();

    void setElement(WebElement element);

    String getId();

    boolean containsText(String text);

    void waitForTextToBePresent(String text);

    void checkTextToBePresent(String text);

    void checkTextToBeNotPresent(String text);
}
