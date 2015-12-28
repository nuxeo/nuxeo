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
package org.nuxeo.ecm.platform.lang.ext.test;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.platform.test.AbstractTranslationTestCase;
import org.nuxeo.ecm.platform.test.TranslationMessagesDiffer;

/**
 * Simple integrity tests on messages file(s).
 *
 * @since 7.3
 */
@RunWith(Parameterized.class)
public class TestExtMessages extends AbstractTranslationTestCase {

    private static final Log log = LogFactory.getLog(TestExtMessages.class);

    private String ext;

    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> configs() throws IOException {
        String lpath = "web/nuxeo.war/WEB-INF/classes";
        String dirpath = FileUtils.getResourcePathFromContext(lpath);
        File dir = new File(dirpath);
        File[] files = dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".properties");
            }
        });
        List<Object[]> res = new ArrayList<>();
        for (File file : files) {
            res.add(new Object[] { lpath + "/" + file.getName() });
        }
        return res;
    }

    public TestExtMessages(String ext) {
        this.ext = ext;
    }

    protected String getEnTranslationsPath() {
        return "crowdin/messages.properties";
    }

    @Test
    public void testFormat() throws IOException {
        checkFormat(ext);
    }

    @Test
    public void testDuplicates() throws IOException {
        checkDuplicates(ext);
    }

    @Test
    public void testDiffExtToRef() throws IOException {
        checkDiff(getEnTranslationsPath(), ext);
    }

    @Test
    public void testDiffRefToExt() throws IOException {
        checkDiff(ext, getEnTranslationsPath());
    }

    /**
     * Overridden to avoid checking ext languages for missing translations, but still log the information.
     */
    @Override
    protected void checkDiff(String path1, String path2) throws IOException {
        Properties p1 = extractProps(path1);
        Properties p2 = extractProps(path2);
        TranslationMessagesDiffer diff = new TranslationMessagesDiffer(p1, p2);
        List<String> missing = diff.getMissingDestKeys();
        if (missing.size() > 0) {
            log.warn(String.format("%s missing translation keys in file at '%s' compared to '%s'.", missing.size(),
                    path2, path1));
            if (log.isDebugEnabled()) {
                log.debug(String.format("Missing keys: '%s'", missing));
            }
        }
    }

}
