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

package org.nuxeo.ecm.webengine.ui.json;

import java.util.Collection;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.nuxeo.common.utils.StringUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class JQueryTreeBuilder<T> {

    public static final String CHILDREN = "children";

    public JSONArray buildTree(String rootName, String path) {
        return buildTree(rootName, path);
    }

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
                    if (off < path.length-1) {
                        jsonChildren = buildChildren(obj, path, off+1);
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
