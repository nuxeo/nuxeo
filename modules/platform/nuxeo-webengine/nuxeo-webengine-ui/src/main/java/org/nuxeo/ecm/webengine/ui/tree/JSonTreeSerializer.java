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

package org.nuxeo.ecm.webengine.ui.tree;

import java.util.Collection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class JSonTreeSerializer implements TreeItemVisitor {

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Must be overridden to provide real URLs
     */
    public String getUrl(TreeItem item) {
        return item.getPath().toString();
    }

    public ArrayNode toJSON(Collection<TreeItem> items) {
        ArrayNode ar = MAPPER.createArrayNode();
        for (TreeItem item : items) {
            ar.add(toJSON(item));
        }
        return ar;
    }

    public ArrayNode toJSON(TreeItem[] items) {
        ArrayNode ar = MAPPER.createArrayNode();
        for (TreeItem item : items) {
            ar.add(toJSON(item));
        }
        return ar;
    }

    public ObjectNode toJSON(TreeItem root) {
        return (ObjectNode) root.accept(this);
    }

    @Override
    public Object visit(TreeItem item) {
        ArrayNode jsons = null;
        if (item.isExpanded()) {
            TreeItem[] children = item.getChildren();
            if (children != null && children.length > 0) {
                jsons = MAPPER.createArrayNode();
                for (TreeItem child : children) {
                    ObjectNode childJson = (ObjectNode) visit(child);
                    jsons.add(childJson);
                }
            }
        }
        return item2JSON(item, jsons);
    }

    /**
     * You may override this method to change the output JSON.
     */
    protected ObjectNode item2JSON(TreeItem item, ArrayNode children) {
        ObjectNode json = MAPPER.createObjectNode();
        json.put("text", item.getLabel()).put("id", item.getPath().toString()).put("href", getUrl(item));
        json.put("expanded", item.isExpanded());
        if (item.isContainer()) {
            if (item.isContainer()) {
                if (item.hasChildren()) {
                    json.set("children", children);
                } else {
                    json.put("hasChildren", true);
                }
            } else {
                json.put("hasChildren", false);
            }
        }
        return json;
    }

}
