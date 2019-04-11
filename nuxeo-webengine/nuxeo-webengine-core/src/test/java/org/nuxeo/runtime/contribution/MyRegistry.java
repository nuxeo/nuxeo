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
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.runtime.contribution.impl.AbstractContributionRegistry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class MyRegistry extends AbstractContributionRegistry<String, MyObject> {

    protected final Map<String, MyObject> map = new HashMap<>();

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
                object.list = new ArrayList<>();
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
