/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.common.utils;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

import org.nuxeo.common.utils.i18n.Labeler;

public class TestLabeler {

    @Test
    public void testMakeLabel() {
        String prefix = "some.prefix";
        Labeler l = new Labeler(prefix);

        assertEquals("some.prefix.item", l.makeLabel("item"));
        assertEquals("some.prefix.item", l.makeLabel("Item"));
    }

}
