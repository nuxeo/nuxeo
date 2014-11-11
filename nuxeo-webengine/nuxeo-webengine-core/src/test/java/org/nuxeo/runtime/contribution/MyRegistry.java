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
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.runtime.contribution.impl.AbstractContributionRegistry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class MyRegistry extends AbstractContributionRegistry<String,MyObject> {

    protected final Map<String, MyObject> map = new HashMap<String, MyObject>();

    @Override
    protected MyObject clone(MyObject object) {
        MyObject clone = new MyObject();
        applyFragment(clone, object);
        return clone;
    }

    @Override
    protected void applyFragment(MyObject object, MyObject fragment) {
        if (fragment.str != null) {
            object.str = fragment.str;
        }
        if (fragment.list != null) {
            if (object.list == null) {
                object.list = new ArrayList<String>();
            }
            object.list.addAll(fragment.list);
        }
        object.id = fragment.id;
    }

    @Override
    protected void installContribution(String key, MyObject object) {
        map.put(key, object);
    }

    @Override
    protected void updateContribution(String key, MyObject object, MyObject oldValue) {
        map.put(key, object);
    }

    @Override
    protected void uninstallContribution(String key, MyObject value) {
        map.remove(key);
    }

    @Override
    protected boolean isMainFragment(MyObject object) {
        return true;
    }

    @Override
    public String toString() {
        return map.toString();
    }

}
