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
 *     Anahide Tchertchian
 */
package org.nuxeo.ftest.formsLayoutDemo;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.junit.Test;
import org.nuxeo.functionaltests.AbstractTest;
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
    public void testDocumentValidationInvalid() {
        ValidationPage page = get(ValidationPage.PAGE_PATH, ValidationPage.class);
        page.fillLayoutInvalid();
        page.submit();
        assertTrue(page.hasGlobalError());
        page.checkLayoutInvalid();
    }

    @Test
    public void testDocumentValidationValid() {
        ValidationPage page = get(ValidationPage.PAGE_PATH, ValidationPage.class);
        page.fillLayoutValid();
        assertFalse(page.hasValidated());
        page.submit();
        assertTrue(page.hasValidated());
        page.checkLayoutValid();
    }

}