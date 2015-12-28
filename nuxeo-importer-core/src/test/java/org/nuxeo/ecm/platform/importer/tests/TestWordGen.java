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

package org.nuxeo.ecm.platform.importer.tests;

import org.junit.Ignore;
import org.junit.Test;
import org.nuxeo.ecm.platform.importer.random.DictionaryHolder;
import org.nuxeo.ecm.platform.importer.random.HunspellDictionaryHolder;
import org.nuxeo.ecm.platform.importer.random.RandomTextGenerator;

// ignored as it takes a lot of time for nothing
@Ignore
public class TestWordGen {

    protected static void print(String string) {
        // System.out.println(string);
    }

    @Test
    public void testWordGen() throws Exception {
        long t0 = System.currentTimeMillis();
        DictionaryHolder dic = new HunspellDictionaryHolder("fr_FR.dic");
        RandomTextGenerator gen = new RandomTextGenerator(dic);
        long t1 = System.currentTimeMillis();
        print("loading dico :" + (t1 - t0) + "ms");
        print("prefilling cache ...");
        gen.prefilCache();
        long t2 = System.currentTimeMillis();
        print("cache prefill time :" + ((t2 - t1) / 1000) + "s");

        int nbFiles = 2000;
        for (int i = 0; i < nbFiles; i++) {
            gen.getRandomText();
        }
        long t3 = System.currentTimeMillis();
        if (t3 == t2) {
            t3++;
        }

        print("generated files in " + ((t3 - t2) / 1000) + "s");
        float rate = nbFiles * 1000 / (t3 - t2);
        print(rate + " files/s");
    }

    @Test
    public void testTextGen() throws Exception {
        long t0 = System.currentTimeMillis();
        DictionaryHolder dic = new HunspellDictionaryHolder("fr_FR.dic");
        RandomTextGenerator gen = new RandomTextGenerator(dic);
        long t1 = System.currentTimeMillis();
        print("loading dico :" + (t1 - t0) + "ms");
        print("prefilling cache ...");
        gen.prefilCache();
        long t2 = System.currentTimeMillis();
        print("cache prefill time :" + ((t2 - t1) / 1000) + "s");

        String txt1000 = gen.getRandomText(1000);
        String txt500 = gen.getRandomText(500);
        String txt50 = gen.getRandomText(50);
        print("txt1000  " + txt1000.length());
        print("txt500  " + txt500.length());
        print("txt50  " + txt50.length());
    }
}
