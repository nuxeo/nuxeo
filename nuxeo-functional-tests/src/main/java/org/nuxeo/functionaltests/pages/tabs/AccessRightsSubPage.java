/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Sun Seng David TAN <stan@nuxeo.com>
 *     Antoine Taillefer
 */
package org.nuxeo.functionaltests.pages.tabs;

import java.util.List;

import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.Required;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NotFoundException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.FindBys;
import org.openqa.selenium.support.pagefactory.ByChained;
import org.openqa.selenium.support.ui.Clock;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.SystemClock;

/**
 * @author Sun Seng David TAN <stan@nuxeo.com>
 *
 */
public class AccessRightsSubPage extends AbstractPage {

    @Required
    @FindBy(id = "add_rights_form:nxl_user_group_suggestion:nxw_selection_suggest")
    WebElement userSelectionSuggestInputText;

    @Required
    @FindBy(id = "add_rights_form:rights_grant_select")
    WebElement selectGrantElement;

    @Required
    @FindBys({ @FindBy(id = "add_rights_form:rights_grant_select"),
            @FindBy(xpath = "//option[@value='Grant']") })
    WebElement selectGrantOption;

    @Required
    @FindBys({ @FindBy(id = "add_rights_form:rights_grant_select"),
            @FindBy(xpath = "//option[@value='Deny']") })
    WebElement selectDenyOption;

    @Required
    @FindBy(id = "add_rights_form:rights_permission_select")
    WebElement selectPermissionElement;

    @Required
    @FindBy(id = "add_rights_form:rights_add_button")
    WebElement addButton;

    @FindBy(id = "validate_rights:document_rights_validate_button")
    WebElement validateButton;

    public AccessRightsSubPage(WebDriver driver) {
        super(driver);
    }

    public boolean hasPermissionForUser(String permission, String username) {
        List<WebElement> trElements = driver.findElements(new ByChained(
                By.className("dataOutput"), By.tagName("tr")));
        boolean hasPermission = false;
        for (WebElement trElement : trElements) {
            List<WebElement> tds = trElement.findElements(By.tagName("td"));
            if (tds.size() > 3) {
                String aceUsername = tds.get(1).getText();
                String aceGrantedPerm = tds.get(2).getText();
                String aceDeniedPerm = tds.get(3).getText();

                if (username.equals(aceUsername)) {
                    if (aceGrantedPerm.equals(permission)
                            || "Manage everything".equals(aceGrantedPerm)) {
                        hasPermission = true;
                    } else {
                        if (aceDeniedPerm.equals(permission)
                                || "Manage everything".equals(aceDeniedPerm)) {
                            hasPermission = false;
                        }
                    }
                }

            }

        }

        // get all the ace

        return hasPermission;
    }

    public AccessRightsSubPage addPermissionForUser(String username,
            String permission, boolean grant) {

        userSelectionSuggestInputText.sendKeys(username);

        // Avoid NoSuchElementException when user suggestion can not be found
        // even after timeout.
        Clock clock = new SystemClock();
        long end = clock.laterBy(AbstractTest.AJAX_TIMEOUT_SECONDS * 1000 * 4);
        WebDriverException lastException = null;
        while (clock.isNowBefore(end)) {
            try {
                findElementWaitUntilEnabledAndClick(
                        By.xpath("//table[@id='add_rights_form:nxl_user_group_suggestion:nxw_selection_suggestionBox:suggest']/tbody/tr[1]/td[2]"),
                        AbstractTest.AJAX_TIMEOUT_SECONDS * 1000,
                        AbstractTest.AJAX_TIMEOUT_SECONDS * 1000);
                lastException = null;
                break;
            } catch (NotFoundException nfe) {
                userSelectionSuggestInputText.sendKeys(Keys.ARROW_DOWN);
                lastException = nfe;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        }
        if (lastException != null) {
            throw lastException;
        }

        Select selectGrant = new Select(selectGrantElement);

        if (grant) {
            selectGrant.selectByValue("Grant");

        } else {
            selectGrant.selectByValue("Deny");
        }

        Select selectPermission = new Select(selectPermissionElement);
        selectPermission.selectByVisibleText(permission);

        addButton.click();

        return asPage(AccessRightsSubPage.class).saveChanges();

    }

    public AccessRightsSubPage saveChanges() {
        waitUntilEnabled(validateButton);
        validateButton.click();
        return asPage(AccessRightsSubPage.class);
    }

}
