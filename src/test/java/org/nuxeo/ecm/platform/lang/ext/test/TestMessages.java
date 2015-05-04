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
package org.nuxeo.ecm.platform.lang.ext.test;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Properties;

import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.platform.test.TranslationMessagesDiffer;

import static org.junit.Assert.assertNotNull;

/**
 * Simple integrity tests on messages file(s).
 *
 * @since 7.3
 */
public class TestMessages {

    @Test
    public void testTranslationsLoading() throws IOException {
        String dirpath = FileUtils.getResourcePathFromContext("web/nuxeo.war/WEB-INF/classes");
        File dir = new File(dirpath);
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".properties");
            }
        });

        for (File file : files) {
            Properties p = TranslationMessagesDiffer.extractProps(file);
            assertNotNull(p);
        }
    }

}
