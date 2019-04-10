/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Yannis JULIENNE
 */
package org.nuxeo.functionaltests;

import static org.junit.Assert.assertEquals;
import java.util.List;
import org.junit.Test;
import org.nuxeo.functionaltests.pages.AbstractPage;
import org.nuxeo.functionaltests.pages.ErrorPage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * Facelet errors tests.
 */
public class ITSeamErrorTest extends AbstractTest {

    private static final String GO_TO_ERROR_PAGE = "Go to error page";

    private static final String TRANSACTION_FAILED_MESSAGE = "Transaction failed";

    private static final String VALIDATION_FAILED_MESSAGE = "Application validation failed, rollingback";

    private static final String PAGE_NOT_UP_TO_DATE_MESSAGE = "This page may not be up to date as an other concurrent request is still running. You can refresh the page later.";

    private static final String COULD_NOT_BE_PROCESSED_MESSAGE = "This request could not be processed because another request is currently being processed. Please wait for the first request to go through.";

    @Test
    public void testSeamActionRollback() throws Exception {
        driver.get(NUXEO_URL);
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText(GO_TO_ERROR_PAGE));
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText("Seam action simple Rollback"));
        String warningMessage = AbstractPage.findElementWithTimeout(
                By.xpath("//div[contains(@class, 'warningFeedback')]/div[@class='ambiance-title']")).getText();
        assertEquals(TRANSACTION_FAILED_MESSAGE, warningMessage);
    }

    @Test
    public void testSeamActionRollbackViaRecoverableClientException() throws Exception {
        driver.get(NUXEO_URL);
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText(GO_TO_ERROR_PAGE));
        AbstractPage.findElementWaitUntilEnabledAndClick(
                By.linkText("Seam action Rollback via RecoverableClientException"));
        String errorMessage = asPage(ErrorPage.class).getErrorFeedbackMessage();
        assertEquals(VALIDATION_FAILED_MESSAGE, errorMessage);
    }

    @Test
    public void testSeamConcurencyIssues() throws Exception {
        driver.get(NUXEO_URL);
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText(GO_TO_ERROR_PAGE));
        AbstractPage.findElementWaitUntilEnabledAndClick(By.linkText("Test Seam concurrency issues"));

        // click the link and stop loading after 3 seconds
        WebElement longBlockingLink = driver.findElement(By.linkText("Seam long blocking call"));
        driver.executeScript("arguments[0].click();setTimeout(function(){window.stop();}, 3000);", longBlockingLink);

        // refresh page
        driver.findElement(By.linkText("Refresh this page")).click();

        // check message
        List<WebElement> messages = AbstractPage.findElementsWithTimeout(By.xpath("//body/ul/li"));
        assertEquals(1, messages.size());
        assertEquals(PAGE_NOT_UP_TO_DATE_MESSAGE, messages.get(0).getText());

        // click again
        driver.findElement(By.linkText("Seam long blocking call")).click();

        // check messages
        messages = AbstractPage.findElementsWithTimeout(By.xpath("//body/ul/li"));
        assertEquals(2, messages.size());
        assertEquals(COULD_NOT_BE_PROCESSED_MESSAGE, messages.get(0).getText());
        assertEquals(PAGE_NOT_UP_TO_DATE_MESSAGE, messages.get(1).getText());
    }
}
