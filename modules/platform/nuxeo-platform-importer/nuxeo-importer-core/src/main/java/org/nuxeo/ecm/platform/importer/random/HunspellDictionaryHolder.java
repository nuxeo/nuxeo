/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Michaël Vachette
 *
 */
package org.nuxeo.ecm.platform.importer.random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Thierry Delprat
 */
public class HunspellDictionaryHolder implements DictionaryHolder {

    protected static final String DEFAULT_DIC = "fr_FR.dic";

    protected static final int INITIAL_SIZE = 100000;

    protected List<String> words = new ArrayList<>(INITIAL_SIZE);

    protected static final Random RANDOM = new Random(); // NOSONAR (doesn't need cryptographic strength)

    protected int wordCount;

    protected String dicName = DEFAULT_DIC;

    public static final Log log = LogFactory.getLog(HunspellDictionaryHolder.class);

    public HunspellDictionaryHolder(String lang) {
        if (lang != null) {
            // sanitize the input so we don't open a security breach.
            dicName = lang.replaceAll("\\W+", "") + ".dic";
        }
    }

    @Override
    public void init() throws IOException {
        loadDic();
        wordCount = words.size();
    }

    /**
     * @deprecated since 6.0
     */
    @Deprecated
    protected void loadDic(String dicName) throws IOException {
        this.dicName = dicName;
        loadDic();
    }

    /**
     * @since 6.0
     */
    protected void loadDic() throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(dicName);
        if (url == null) {
            log.error("not found: " + dicName);
            return;
        }
        try (InputStream in = url.openStream(); BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int idx = line.indexOf("/");
                if (idx > 0) {
                    String word = line.substring(0, idx);
                    words.add(word + " ");
                } else {
                    words.add(line + " ");
                }
            }
        }
    }

    @Override
    public int getWordCount() {
        return wordCount;
    }

    @Override
    public String getRandomWord() {
        int idx = RANDOM.nextInt(wordCount);
        return words.get(idx);
    }
}
