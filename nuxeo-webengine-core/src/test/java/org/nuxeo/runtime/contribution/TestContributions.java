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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.runtime.contribution;

import java.util.ArrayList;

import junit.framework.TestCase;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class TestContributions extends TestCase {

    protected MyRegistry reg;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        reg = new MyRegistry();
    }

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

//        reg.remove("key1");
//        System.out.println(reg);

        reg.removeFragment("key1", mf2);
        // System.out.println(reg);
    }

}
