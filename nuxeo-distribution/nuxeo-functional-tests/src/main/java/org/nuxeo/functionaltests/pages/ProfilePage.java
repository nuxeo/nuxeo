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

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * @since 7.3
 */
public class ProfilePage extends AbstractPage {

	@FindBy(id = "userProfileDropDownMenu")
	WebElement actionsLink;
	
	@FindBy(id = "userProfileButtons:changePasswordButton")
	WebElement changePasswordLink;

	public ProfilePage(final WebDriver driver) {
		super(driver);
	}

	public OwnUserChangePasswordFormPage getChangePasswordUserTab() {
		actionsLink.click();
		changePasswordLink.click();
		return asPage(OwnUserChangePasswordFormPage.class);
	}

}
