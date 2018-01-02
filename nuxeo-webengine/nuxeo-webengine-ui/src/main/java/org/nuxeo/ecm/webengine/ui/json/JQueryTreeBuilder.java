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

package org.nuxeo.ecm.webengine.ui.json;

import java.util.Collection;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.nuxeo.common.utils.StringUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public abstract class JQueryTreeBuilder<T> {

    public static final String CHILDREN = "children";

    public JSONArray buildTree(T root, String path) {
        if (path == null || path.length() == 0 || "/".equals(path)) {
            return buildChildren(root);
        }
        String[] ar = StringUtils.split(path, '/', false);
        if (ar.length > 1) {
            String name = getName(root);
            if (name.equals(ar[0])) {
                return buildChildren(root, ar, 1);
            }
        }

        return buildChildren(root);
    }

    public JSONArray buildChildren(T parent) {
        JSONArray json = new JSONArray();
        Collection<T> children = getChildren(parent);
        if (children != null) {
            for (T obj : children) {
                JSONObject map = toJson(obj);
                json.add(map);
            }
        }
        return json;
    }

    public JSONArray buildChildren(T parent, String[] path, int off) {
        JSONArray json = new JSONArray();
        String expandName = path[off];
        Collection<T> children = getChildren(parent);
        if (children != null) {
            for (T obj : children) {
                JSONObject map = toJson(obj);
                String childName = getName(obj);
                if (expandName.equals(childName)) {
                    JSONArray jsonChildren = null;
                    if (off < path.length - 1) {
                        jsonChildren = buildChildren(obj, path, off + 1);
                    } else {
                        jsonChildren = buildChildren(obj);
                    }
                    map.element(CHILDREN, jsonChildren);
                }
                json.add(map);
            }
        }
        return json;
    }

    protected abstract T getObject(String name);

    protected abstract String getName(T obj);

    protected abstract Collection<T> getChildren(T obj);

    protected abstract JSONObject toJson(T obj);

}
