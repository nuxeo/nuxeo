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
package org.nuxeo.ecm.platform.test;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.FileUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Base test class with helpers for translation files.
 *
 * @since 7.3
 */
public abstract class AbstractTranslationTestCase {

    private static final Log log = LogFactory.getLog(AbstractTranslationTestCase.class);

    protected InputStream getFromContext(String path) throws IOException {
        String file = FileUtils.getResourcePathFromContext(path);
        return new FileInputStream(file);

    }

    protected TranslationProperties extractProps(String path) throws IOException {
        try (InputStream in = getFromContext(path)) {
            return extractProps(in);
        }
    }

    protected TranslationProperties extractProps(InputStream in) throws IOException {
        TranslationProperties props = new TranslationProperties();
        props.load(in);
        return props;
    }

    protected void checkFormat(String path) throws IOException {
        TranslationProperties p = extractProps(path);
        assertNotNull(p);
        checkSingleLabels(path, p);
        // TODO: check encoding?
    }

    protected void checkSingleLabels(String path, TranslationProperties p) throws IOException {
        // maybe refine when dealing with long labels, just warn in case labels are not well chosen
        Set<String> single = p.getSingleLabels();
        if (single.size() > 0) {
            log.warn(String.format("%s single translation keys in file at '%s'.", single.size(), path));
            if (log.isDebugEnabled()) {
                log.debug(String.format("Single keys: '%s'", single));
            }
        }
    }

    protected void checkDuplicates(String path, String... allowed) throws IOException {
        TranslationProperties r = extractProps(path);
        if (allowed != null && allowed.length > 0) {
            assertEquals(String.format("Unexpected duplicates in file at '%s'", path), Arrays.asList(allowed),
                    r.getDuplicates());
        } else {
            assertEquals(String.format("Duplicates in file at '%s': %s", path, r.getDuplicates().toString()), 0,
                    r.getDuplicates().size());
        }
    }

    protected void checkDiff(String path1, String path2) throws IOException {
        Properties p1 = extractProps(path1);
        Properties p2 = extractProps(path2);
        TranslationMessagesDiffer diff = new TranslationMessagesDiffer(p1, p2);
        List<String> missing = diff.getMissingDestKeys();
        assertEquals(
                String.format("Missing translation keys in file at '%s' compared to '%s': %s", path2, path1, missing),
                0, missing.size());
    }

}
