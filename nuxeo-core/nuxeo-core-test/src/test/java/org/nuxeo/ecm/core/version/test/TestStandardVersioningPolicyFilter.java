/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.ecm.core.version.test;

import org.junit.Test;
import org.nuxeo.ecm.core.versioning.StandardVersioningPolicyFilter;

import static org.junit.Assert.assertEquals;

/**
 * @since 9.1
 */
public class TestStandardVersioningPolicyFilter {

    @Test
    public void testEvaluateCondition() {

        String cond1 = "previousDocument.dc.title != currentDocument.dc.title";
        String expectedCond1 = "#{previousDocument != null && previousDocument.dc.title != currentDocument.dc.title}";
        assertEquals(expectedCond1, StandardVersioningPolicyFilter.evaluateCondition(cond1));

        String cond2 = "#{previousDocument.dc.title != currentDocument.dc.title}";
        String expectedCond2 = "#{previousDocument != null && previousDocument.dc.title != currentDocument.dc.title}";
        assertEquals(expectedCond2, StandardVersioningPolicyFilter.evaluateCondition(cond2));

        String cond3 = "previousDocument  !=  null && previousDocument.dc.title != currentDocument.dc.title";
        String expectedCond3 = "#{" + cond3 + "}";
        assertEquals(expectedCond3, StandardVersioningPolicyFilter.evaluateCondition(cond3));

        String cond4 = "${previousDocument.dc.title != currentDocument.dc.title || previousDocument == null }";
        assertEquals(cond4, StandardVersioningPolicyFilter.evaluateCondition(cond4));

    }
}
