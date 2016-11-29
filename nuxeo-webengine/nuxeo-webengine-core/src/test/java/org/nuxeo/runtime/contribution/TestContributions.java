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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.runtime.contribution;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class TestContributions {

    protected MyRegistry reg;

    @Before
    public void setUp() throws Exception {
        reg = new MyRegistry();
    }

    @Test
    public void test1() {
        MyObject mf = new MyObject("key1", "#1");
        mf.str = "a";
        reg.addFragment(mf.id, mf);

        MyObject mf2 = new MyObject("key1", "#2");
        mf2.str = "b";
        mf2.list = new ArrayList<String>();
        mf2.list.add("k1");
        reg.addFragment(mf2.id, mf2);

        // System.out.println(reg);
    }

    @Test
    public void test2() {
        MyObject mf = new MyObject("key1", "#1");
        mf.str = "a";
        reg.addFragment(mf.id, mf, "base");

        MyObject base = new MyObject("base", "#1");
        base.str = "base str";
        base.list = new ArrayList<String>();
        base.list.add("base1");
        reg.addFragment(base.id, base);

        MyObject mf2 = new MyObject("key1", "#2");
        mf2.str = "b";
        mf2.list = new ArrayList<String>();
        mf2.list.add("k1");
        reg.addFragment(mf2.id, mf2);

        // System.out.println(reg);
    }

    @Test
    public void test3() {
        MyObject mf = new MyObject("key1", "#1");
        mf.str = "a";
        reg.addFragment(mf.id, mf, "base");

        MyObject base = new MyObject("base", "#1");
        base.str = "base str";
        base.list = new ArrayList<String>();
        base.list.add("base1");
        reg.addFragment(base.id, base);

        MyObject mf2 = new MyObject("key1", "#2");
        mf2.str = "b";
        mf2.list = new ArrayList<String>();
        mf2.list.add("k1");
        reg.addFragment(mf2.id, mf2);

        // reg.remove("key1");
        // System.out.println(reg);

        reg.removeFragment("key1", mf2);
        // System.out.println(reg);
    }

}
