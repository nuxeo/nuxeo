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
 *     pierre
 */
package org.nuxeo.ecm.platform.picture.core.im;

import static org.junit.Assert.assertEquals;

import java.awt.Point;

import org.junit.Test;
import org.nuxeo.ecm.platform.picture.magick.utils.ImageResizer;

/**
 * @since 10.3
 */
public class TestImageResizer {

    @Test
    public void testAdaptSize() {
        test(200, 100, 200, 200, 100);
        test(400, 100, 200, 200, 50);
        test(100, 400, 200, 50, 200);
        test(200, 100, -10, 200, 100);
    }

    protected void test(int originalW, int originalH, int max, int expectedW, int expectedH) {
        Point p = ImageResizer.scaleToMax(originalW, originalH, max);
        assertEquals(expectedW, p == null ? originalW : p.getX(), 0d);
        assertEquals(expectedH, p == null ? originalH : p.getY(), 0d);
    }

}
