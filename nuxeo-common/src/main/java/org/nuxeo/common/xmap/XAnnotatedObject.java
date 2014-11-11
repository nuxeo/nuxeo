/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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

package org.nuxeo.common.xmap;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XObject;
import org.w3c.dom.Element;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class XAnnotatedObject {

    final XMap xmap;
    final Class<?> klass;
    final Constructor<?> ctor;
    final Path path;

    final List<XAnnotatedMember> members;

    Sorter sorter;

    public XAnnotatedObject(XMap xmap, Class<?> klass, XObject xob) {
        try {
            this.xmap = xmap;
            this.klass = klass;
            this.ctor = this.klass.getDeclaredConstructor();
            ctor.setAccessible(true);
            path = new Path(xob.value());
            members = new ArrayList<XAnnotatedMember>();
            String[] order = xob.order();
            if (order.length > 0) {
                sorter = new Sorter(order);
            }
        } catch (Exception e) {
            throw new RuntimeException("Invalid xmap class - no default constructor found", e);
        }
    }

    public void addMember(XAnnotatedMember member) {
        members.add(member);
    }

    public Path getPath() {
        return path;
    }

    public Object newInstance(Context ctx, Element element) throws Exception {
        Object ob = ctor.newInstance();
        ctx.push(ob);

        if (sorter != null) {
            Collections.sort(members, sorter);
            sorter = null; // sort only once
        }

        // set annotated members
        for (XAnnotatedMember member : members) {
            member.process(ctx, element);
        }

        return ctx.pop();
    }
}

class Sorter implements Comparator<XAnnotatedMember>, Serializable {

    private static final long serialVersionUID = -2546984283687927308L;

    private final Map<String, Integer> order = new HashMap<String, Integer>();

    Sorter(String[] order) {
        for (int i = 0; i < order.length; i++) {
            this.order.put(order[i], i);
        }
    }

    public int compare(XAnnotatedMember o1, XAnnotatedMember o2) {
        String p1 = o1.path == null ? "" : o1.path.path;
        String p2 = o2.path == null ? "" : o2.path.path;
        Integer order1 = order.get(p1);
        Integer order2 = order.get(p2);
        int n1 = order1 == null ? Integer.MAX_VALUE : order1;
        int n2 = order2 == null ? Integer.MAX_VALUE : order2;
        return n1 - n2;
    }

}
