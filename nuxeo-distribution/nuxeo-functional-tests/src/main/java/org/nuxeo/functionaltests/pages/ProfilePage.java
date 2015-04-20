/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Maxime HILAIRE
 */
package org.nuxeo.functionaltests.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * @since 7.3
 */
public class ProfilePage extends AbstractPage {

	public ProfilePage(final WebDriver driver) {
		super(driver);
	}

	public OwnUserChangePasswordFormPage getChangePasswordUserTab() {
		WebElement actionsLink = findElementWithTimeout(By.id("userProfileDropDownMenu"));
		actionsLink.click();
		WebElement changePasswordLink = findElementWithTimeout(By.id("userProfileButtons:changePasswordButton"));
		changePasswordLink.click();
		return asPage(OwnUserChangePasswordFormPage.class);
	}

}
