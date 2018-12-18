/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thomas Roger
 */

package org.nuxeo.platform.el;

import static org.junit.Assert.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.Test;
import org.nuxeo.ecm.platform.el.FieldAdapterManager;

/**
 * @since 10.10
 */
public class TestFieldAdapterManager {

    @Test
    public void testGetComponentTypeForDisplay() {
        assertEquals(String.class, FieldAdapterManager.getComponentTypeForDisplay(String.class));
        assertEquals(Long.class, FieldAdapterManager.getComponentTypeForDisplay(Long.class));
        assertEquals(Date.class, FieldAdapterManager.getComponentTypeForDisplay(Date.class));
        assertEquals(Date.class, FieldAdapterManager.getComponentTypeForDisplay(Calendar.class));
        assertEquals(Date.class, FieldAdapterManager.getComponentTypeForDisplay(GregorianCalendar.class));
    }
}
