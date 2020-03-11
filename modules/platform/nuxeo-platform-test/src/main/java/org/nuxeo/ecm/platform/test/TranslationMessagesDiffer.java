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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Differ for translation files.
 *
 * @since 7.3
 */
public class TranslationMessagesDiffer {

    protected final Properties src;

    protected final Properties dst;

    public TranslationMessagesDiffer(Properties src, Properties dst) {
        super();
        this.src = src;
        this.dst = dst;
    }

    public List<String> getMissingDestKeys() {
        return getMissingKeys(src, dst);
    }

    public List<String> getAdditionalDestKeys() {
        return getMissingKeys(dst, src);
    }

    protected List<String> getMissingKeys(Properties src, Properties dst) {
        List<String> missing = new ArrayList<>();
        for (Object item : src.keySet()) {
            if (!dst.containsKey(item)) {
                missing.add((String) item);
            }
        }
        Collections.sort(missing);
        return missing;
    }

    public static TranslationProperties extractProps(File file) throws IOException {
        TranslationProperties props = new TranslationProperties();
        try (InputStream in = new FileInputStream(file)) {
            props.load(in);
        }
        return props;
    }

}
