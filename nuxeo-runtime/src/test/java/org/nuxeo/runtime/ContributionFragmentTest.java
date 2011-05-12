/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.nuxeo.runtime.model.ContributionFragmentRegistry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ContributionFragmentTest extends TestCase {

    static class MyContrib {
        protected String id;
        protected String title;
        protected List<String> args;
        public MyContrib(String id) {
            this.id = id;
        }
        public String getId() {
            return id;
        }
        public String getTitle() {
            return title;
        }
        public List<String> getArgs() {
            return args;
        }
    }

    static class MyRegistry extends ContributionFragmentRegistry<MyContrib> {
        Map<String, MyContrib> registry = new HashMap<String, MyContrib>();
        @Override
        public String getContributionId(MyContrib contrib) {
            return contrib.getId();
        }
        @Override
        public void contributionUpdated(
                String id,
                MyContrib contrib, MyContrib origContrib) {
            registry.put(id, contrib);
        }
        @Override
        public void contributionRemoved(String id, MyContrib origContrib) {
            registry.remove(id);
        }
        @Override
        public MyContrib clone(MyContrib object) {
            MyContrib clone = new MyContrib(object.getId());
            clone.title = object.title;
            if (object.args != null) {
                clone.args = new ArrayList<String>(object.args);
            }
            return clone;
        }
        @Override
        public void merge(MyContrib src, MyContrib dst) {
            dst.title = src.title;
            if (dst.args == null) {
                dst.args = new ArrayList<String>();
            }
            if (src.args != null) {
                dst.args.addAll(src.args);
            }
        }
        public Map<String, MyContrib> getRegistry() {
            return registry;
        }
    }

    public static List<String> newList(String ... args) {
        return Arrays.asList(args);
    }


    public void testRegistry() throws Exception {
        MyRegistry reg = new MyRegistry();
        MyContrib c1 = new MyContrib("c1");
        c1.title = "c1 title";
        reg.addContribution(c1);
        MyContrib c11 = new MyContrib("c1");
        c11.title = "c11 title";
        c11.args = new ArrayList<String>(Arrays.asList(new String[]{"a", "b"}));
        reg.addContribution(c11);

        MyContrib c2 = new MyContrib("c2");
        c2.title = "c2 title";
        c2.args = new ArrayList<String>(Arrays.asList(new String[]{"a", "b"}));
        reg.addContribution(c2);
        MyContrib c21 = new MyContrib("c2");
        c21.args = new ArrayList<String>(Arrays.asList(new String[]{"c", "d"}));
        reg.addContribution(c21);

        assertEquals("c1", reg.getRegistry().get("c1").getId());
        assertEquals("c11 title", reg.getRegistry().get("c1").getTitle());
        assertEquals(newList("a", "b"), reg.getRegistry().get("c1").getArgs());

        assertEquals("c2", reg.getRegistry().get("c2").getId());
        assertNull(reg.getRegistry().get("c2").getTitle());
        assertEquals(newList("a", "b", "c", "d"), reg.getRegistry().get("c2").getArgs());

        reg.removeContribution(c21);

        assertEquals("c2", reg.getRegistry().get("c2").getId());
        assertEquals("c2 title", reg.getRegistry().get("c2").getTitle());
        assertEquals(newList("a", "b"), reg.getRegistry().get("c2").getArgs());

        reg.removeContribution(c2);
        assertNull(reg.getRegistry().get("c2"));

        assertEquals("c1", reg.getRegistry().get("c1").getId());
        assertEquals("c11 title", reg.getRegistry().get("c1").getTitle());
        assertEquals(newList("a", "b"), reg.getRegistry().get("c1").getArgs());

        assertEquals(1, reg.getRegistry().size());
    }

}
