/*
 * (C) Copyright 2021 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Kevin Leturc <kleturc@nuxeo.com>
 */

package org.nuxeo.ecm.core.storage;

import org.junit.Test;

import java.util.Arrays;
import java.util.function.Function;

import static org.junit.Assert.assertEquals;

/**
 * @since 11.5
 */
public class TestFulltextExtractorWork {

    @Test
    public void testJoinText() {
        FulltextExtractorWork work = new FulltextExtractorWork("dummy", "dummy", true, true, true);

        String text = work.joinText(Arrays.asList("string1", "string2"), Function.identity(), 100);
        assertEquals(" string1 string2 ", text);

        text = work.joinText(Arrays.asList("string1", "string2"), Function.identity(), 17);
        assertEquals(" string1 string2 ", text);

        text = work.joinText(Arrays.asList("string1", "string2"), Function.identity(), 10);
        assertEquals(" string1 s", text);

        text = work.joinText(Arrays.asList("string1", "string2"), Function.identity(), 9);
        assertEquals(" string1 ", text);

        text = work.joinText(Arrays.asList("string1", "string2"), Function.identity(), 8);
        assertEquals(" string1", text);

        text = work.joinText(Arrays.asList("string1", "string2"), Function.identity(), 7);
        assertEquals(" string", text);

        text = work.joinText(Arrays.asList("string1", "string2"), Function.identity(), 1);
        assertEquals(" ", text);
    }
}
