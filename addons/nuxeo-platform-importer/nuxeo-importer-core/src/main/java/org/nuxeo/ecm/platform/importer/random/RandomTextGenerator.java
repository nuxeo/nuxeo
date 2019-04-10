/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.importer.random;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.nuxeo.ecm.core.api.NuxeoException;

/**
 * Random text generator to be used for load testing
 *
 * @author Thierry Delprat
 */
public class RandomTextGenerator {

    protected DictionaryHolder dictionaryHolder;

    protected Map<String, String> paragraphCache = new HashMap<String, String>();

    protected Map<String, String> pageCache = new HashMap<String, String>();

    protected Map<String, String> blockCache = new HashMap<String, String>();

    protected static final int PARAGRAPH_CACHE_SIZE = 100;

    protected static final int PARAGRAPH_CACHE_HIT = 100;

    protected static final int PAGE_CACHE_SIZE = 50;

    protected static final int PAGE_CACHE_HIT = 30;

    protected static final int BLOC_CACHE_SIZE = 30;

    protected static final int BLOC_CACHE_HIT = 20;

    protected static final int BLOC_SIZE = 10 * 1024;

    protected static final int NB_WORDS_PER_LINE = 20;

    protected static final int NB_LINES_PER_PARAGRAPH = 40;

    protected static final int NB_PARAGRAPH_PER_PAGE = 8;

    protected static final int NB_PAGE_PER_BLOC = 3;

    protected static final Random RANDOM = new Random(); // NOSONAR (doesn't need cryptographic strength)

    public RandomTextGenerator(DictionaryHolder dictionary) {
        dictionaryHolder = dictionary;
    }

    protected int getTargetPageMaxSizeB() {
        return (int) (1.2 * (BLOC_SIZE / NB_PAGE_PER_BLOC));
    }

    protected int getTargetParagraphMaxSizeB() {
        return (int) (1.2 * (getTargetPageMaxSizeB() / NB_PARAGRAPH_PER_PAGE));
    }

    public String getRandomTitle(int nbWord) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < nbWord; i++) {
            sb.append(dictionaryHolder.getRandomWord());
        }
        return sb.toString();

    }

    public String getRandomLine() {
        int nbW = 10 + RANDOM.nextInt(NB_WORDS_PER_LINE);
        StringBuffer sb = new StringBuffer();

        for (int i = 0; i < nbW; i++) {
            sb.append(dictionaryHolder.getRandomWord());
        }
        sb.append(".\n");
        return sb.toString();
    }

    public String generateParagraph() {
        int nbL = 10 + RANDOM.nextInt(NB_LINES_PER_PARAGRAPH);
        StringBuffer sb = new StringBuffer();

        int maxSize = getTargetParagraphMaxSizeB();

        for (int i = 0; i < nbL; i++) {
            sb.append(getRandomLine());
            if (sb.length() > maxSize) {
                break;
            }
        }
        sb.append("\n\n");
        return sb.toString();
    }

    public void prefilCache() {

        try {
            dictionaryHolder.init();
        } catch (IOException e) {
            throw new NuxeoException(e);
        }

        for (int i = 0; i < PARAGRAPH_CACHE_SIZE; i++) {
            paragraphCache.put("P" + i, generateParagraph());
        }

        for (int i = 0; i < PAGE_CACHE_SIZE; i++) {
            String page = generatePage();
            pageCache.put("P" + i, page);
        }

        for (int i = 0; i < BLOC_CACHE_SIZE; i++) {
            String page = generateBloc();
            blockCache.put("B" + i, page);
        }

    }

    public String getRandomParagraph() {
        int rand = RANDOM.nextInt();
        int idx = RANDOM.nextInt(PARAGRAPH_CACHE_SIZE);
        String paragraph = null;
        if (rand % PARAGRAPH_CACHE_HIT != 0) {
            paragraph = paragraphCache.get("P" + idx);
        }
        if (paragraph == null) {
            paragraph = generateParagraph();
            paragraphCache.put("P" + idx, paragraph);
        }
        return paragraph;
    }

    public String generatePage() {
        int nbL = RANDOM.nextInt(NB_PARAGRAPH_PER_PAGE) + 1;
        StringBuffer sb = new StringBuffer();

        int maxTargetPageSize = getTargetPageMaxSizeB();
        for (int i = 0; i < nbL; i++) {
            sb.append(getRandomParagraph());
            if (sb.length() > maxTargetPageSize) {
                break;
            }
        }
        sb.append("\n\n");
        return sb.toString();
    }

    public String getRandomPage() {
        int rand = RANDOM.nextInt();
        int idx = RANDOM.nextInt(PAGE_CACHE_SIZE);
        String page = null;
        if (rand % PAGE_CACHE_HIT != 0) {
            page = pageCache.get("P" + idx);
        }
        if (page == null) {
            page = generatePage();
            pageCache.put("P" + idx, page);
        }
        return page;
    }

    public String generateBloc() {
        StringBuffer sb = new StringBuffer();

        while (sb.length() < BLOC_SIZE) {
            sb.append(getRandomPage());
        }
        return sb.toString();
    }

    public String getRandomBloc() {
        int rand = RANDOM.nextInt();
        int idx = RANDOM.nextInt(BLOC_CACHE_SIZE);
        String bloc = null;
        if (rand % BLOC_CACHE_HIT != 0) {
            bloc = blockCache.get("B" + idx);
        }
        if (bloc == null) {
            bloc = generateBloc();
            blockCache.put("B" + idx, bloc);
        }
        return bloc;
    }

    public String getRandomText(int avSizeInK) {
        if (avSizeInK == 0) {
            return "";
        }
        StringBuffer sb = new StringBuffer();
        int minSize = (int) (avSizeInK * 1024 * (0.8 + 0.4 * RANDOM.nextFloat()));
        while (sb.length() < (minSize - BLOC_SIZE)) {
            String p = getRandomBloc();
            sb.append(p);
        }
        while (sb.length() < minSize) {
            String p = getRandomPage();
            sb.append(p);
        }
        return sb.toString();
    }

    public String getRandomText() {
        int sizeK = RANDOM.nextInt(500) + 1;
        return getRandomText(sizeK);
    }

}
