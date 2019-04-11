/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.shibboleth.web.tree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Tree node class handling node information and Nodes operation
 *
 * @author <a href="mailto:akervern@nuxeo.com">Arnaud Kervern</a>
 */
public class UserTreeNode {

    protected DocumentModel document;

    protected List<UserTreeNode> childrens;

    protected String name = "";

    public UserTreeNode(String name) {
        this.name = name;
    }

    public UserTreeNode(DocumentModel document) {
        this.document = document;
    }

    public List<UserTreeNode> getChildrens() {
        if (childrens == null) {
            childrens = new ArrayList<>();
        }
        return childrens;
    }

    public void addChildren(UserTreeNode child) {
        getChildrens().add(child);
    }

    public String getId() {
        return document == null ? name : document.getId();
    }

    /**
     * Get the displayed name, if instantiate with a documentModel it will be the document Id
     *
     * @return name defined with the constructor, or Document Id
     */
    public String getDisplayedName() {
        if (name.equals("") && document != null) {
            String id = document.getId();
            int pos = id.lastIndexOf(UserTreeNodeHelper.getParseString());

            if (pos > 0) {
                name = id.substring(pos + UserTreeNodeHelper.getParseStringLength());
            } else {
                name = id;
            }
        }
        return name;
    }

    /**
     * Factory method to build a collection of UserTReeNode.
     *
     * @return empty list if no docs passed
     */
    public static List<UserTreeNode> constructNodes(Collection<DocumentModel> docs) {
        List<UserTreeNode> ret = new ArrayList<>();

        if (docs != null) {
            for (DocumentModel doc : docs) {
                ret.add(new UserTreeNode(doc));
            }
        }

        return ret;
    }
}
