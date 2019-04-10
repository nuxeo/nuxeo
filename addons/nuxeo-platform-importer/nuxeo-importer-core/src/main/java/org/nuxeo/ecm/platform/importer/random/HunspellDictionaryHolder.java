/*
 * (C) Copyright 2006-2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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

    protected static final int INITIAL_SIZE = 100000;

    protected List<String> words = new ArrayList<>(INITIAL_SIZE);

    protected Random generator;

    protected int wordCount;

    protected String dicName;

    public static final Log log = LogFactory.getLog(HunspellDictionaryHolder.class);

    public HunspellDictionaryHolder(String dicName) throws Exception {
        generator = new Random(System.currentTimeMillis());
        this.dicName = dicName;
    }

    @Override
    public void init() throws Exception {
        loadDic();
        wordCount = words.size();
    }

    /**
     * @deprecated since 6.0
     */
    @Deprecated
    protected void loadDic(String dicName) throws Exception {
        this.dicName = dicName;
        loadDic();
    }

    /**
     * @since 6.0
     */
    protected void loadDic() throws IOException {
        URL url = Thread.currentThread().getContextClassLoader().getResource(
                dicName);
        if (url == null) {
            log.error("not found: " + dicName);
            return;
        }
        try (InputStream in = url.openStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
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
        int idx = generator.nextInt(wordCount);
        return words.get(idx);
    }
}
