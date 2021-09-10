/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
package org.nuxeo.ftest.formsLayoutDemo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
import org.nuxeo.functionaltests.forms.JSListWidgetElement;
import org.nuxeo.functionaltests.formsLayoutDemo.page.ValidationPage;

/**
 * @since 7.2
 */
public class ITLayoutDocumentValidationTest extends AbstractTest {

    @Test
    public void testDocumentValidationEmpty() {
        ValidationPage page = get(ValidationPage.PAGE_PATH, ValidationPage.class);
        page.resetDemoDocument();
        page.submit();
        assertTrue(page.hasGlobalError());
        page.checkLayoutEmpty();
    }

    @Test
    @Ignore("NXP-30564")
    public void testDocumentValidationInvalid() {
        ValidationPage page = get(ValidationPage.PAGE_PATH, ValidationPage.class);
        page.fillLayoutInvalid();
        page.submit();
        assertTrue(page.hasGlobalError());
        page.checkLayoutInvalid();
    }

    /**
     * NXP-23644
     */
    @Test
    @Ignore
    public void testDocumentValidationInvalidWithRequiredFieldMissing() {
        ValidationPage page = get(ValidationPage.PAGE_PATH, ValidationPage.class);
        page.fillLayoutInvalidWithMissingRequiredField();
        page.submit();
        assertTrue(page.hasGlobalError());
        page.submit();

        JSListWidgetElement llist = page.getLayout().getWidget("nxw_listOfListsWidget", JSListWidgetElement.class);
        JSListWidgetElement firstSubList = llist.getSubWidget("nxw_stringListItem", 0, JSListWidgetElement.class,
                false);

        // Ensure that the field from the second sublist was not copied and duplicated in the first one
        assertEquals(1, firstSubList.getRows().size());
    }

    @Test
    @Ignore("NXP-30564")
    public void testDocumentValidationValid() {
        ValidationPage page = get(ValidationPage.PAGE_PATH, ValidationPage.class);
        page.fillLayoutValid();
        assertFalse(page.hasValidated());
        page.submit();
        assertTrue(page.hasValidated());
        page.checkLayoutValid();
    }

}
