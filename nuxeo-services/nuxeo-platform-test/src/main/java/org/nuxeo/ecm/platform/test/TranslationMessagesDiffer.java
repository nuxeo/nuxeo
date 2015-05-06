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
        InputStream in = new FileInputStream(file);
        TranslationProperties props = new TranslationProperties();
        props.load(in);
        return props;
    }

}
