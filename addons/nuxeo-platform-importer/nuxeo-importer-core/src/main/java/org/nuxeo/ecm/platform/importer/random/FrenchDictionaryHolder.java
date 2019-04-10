/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.importer.random;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

/**
 *
 * @author Thierry Delprat
 *
 */
public class FrenchDictionaryHolder implements DictionaryHolder {

    protected List<String> words = new LinkedList<String>();

    protected Random generator;

    protected int wordCount;

    public FrenchDictionaryHolder() throws Exception {
        generator = new Random(System.currentTimeMillis());
    }

    public void init() throws Exception {
        loadDic("fr_FR.dic");
        wordCount = words.size();
    }

    protected void loadDic(String dicName) throws Exception {

        // File dic = FileUtils.getResourceFileFromContext(dicName);
        URL url = Thread.currentThread().getContextClassLoader().getResource(
                dicName);

        BufferedReader reader = null;
        try {
            // InputStream in = new FileInputStream(dic);
            InputStream in = url.openStream();
            reader = new BufferedReader(new InputStreamReader(in));
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
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.nuxeo.ecm.platform.importer.random.DictionaryHolder#getWordCount()
     */
    public int getWordCount() {
        return wordCount;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * org.nuxeo.ecm.platform.importer.random.DictionaryHolder#getRandomWord()
     */
    public String getRandomWord() {
        int idx = generator.nextInt(wordCount);
        return words.get(idx);
    }

}
