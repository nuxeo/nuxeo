/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Martin Pernollet
 */
package org.nuxeo.ecm.platform.groups.audit.service.rendering.tests;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Workbook;
import org.junit.Test;
import org.nuxeo.ecm.platform.groups.audit.service.acl.excel.ExcelBuilderMultiSheet;

public class TestMultiSheetBuilder extends AbstractAclLayoutTest {
    private final static Log log = LogFactory.getLog(TestMultiSheetBuilder.class);

    protected static File testFile = new File(folder + TestMultiSheetBuilder.class.getSimpleName() + ".xls");

    @Test
    public void testMultiSheet() {
        ExcelBuilderMultiSheet s = new ExcelBuilderMultiSheet();

        assertTrue("", s.getVirtualCellColumn(0) == 0);
        assertTrue("", s.getVirtualCellColumn(255) == 255);
        assertTrue("", s.getVirtualCellColumn(256) == 0);
        assertTrue("", s.getVirtualCellColumn(511) == 255);
        assertTrue("", s.getVirtualCellColumn(512) == 0);

        assertTrue("", s.getVirtualCellSheet(0) == 0);
        assertTrue("", s.getVirtualCellSheet(255) == 0);
        assertTrue("", s.getVirtualCellSheet(256) == 1);
        assertTrue("", s.getVirtualCellSheet(511) == 1);
        assertTrue("", s.getVirtualCellSheet(512) == 2);
    }

    @Test
    public void testMultiSheet2() {
        ExcelBuilderMultiSheet s = new ExcelBuilderMultiSheet();
        s.setMultiSheetColumns(true);

        s.setCell(0, 0, "@ (0,0)"); // @page 0
        s.setCell(0, 255, "@ (0,255)");
        s.setCell(0, 256, "@ (0,256)"); // @page 1
        s.setCell(0, 511, "@ (0,511)");
        s.setCell(0, 512, "@ (0,512)"); // @page 2

        s.setCell(0, 257, "@ (0,257)");
        s.setCell(0, 254, "@ (0,254)");
        s.setCell(0, 513, "@ (0,513)");
        s.setCell(0, 1, "@ (0,1)");

        Workbook w = null;
        try {
            s.save(testFile);
            w = s.load(testFile);
        } catch (IOException e) {
            log.error(e, e);
        } catch (InvalidFormatException e) {
            e.printStackTrace();
        }

        if (w != null) {
            assertTrue("", "@ (0,0)".equals(get(w, 0, 0, 0)));
            assertTrue("", "@ (0,255)".equals(get(w, 0, 0, 255)));
            assertTrue("", "@ (0,256)".equals(get(w, 1, 0, 0)));
            assertTrue("", "@ (0,511)".equals(get(w, 1, 0, 255)));
            assertTrue("", "@ (0,512)".equals(get(w, 2, 0, 0)));
        } else
            fail("not reloaded");
    }

}
