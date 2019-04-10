/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
            childrens = new ArrayList<UserTreeNode>();
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
     * Get the displayed name, if instantiate with a documentModel it
     * will be the document Id
     *
     * @return name defined with the constructor, or Document Id
     */
    public String getDisplayedName() {
        if (name.equals("") && document != null) {
            String id = document.getId();
            int pos = id.lastIndexOf(UserTreeNodeHelper.getParseString());

            if (pos > 0) {
                name = id.substring(pos + UserTreeNodeHelper.getParseStringLength());
            } else{
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
        List<UserTreeNode> ret = new ArrayList<UserTreeNode>();

        if (docs != null) {
            for(DocumentModel doc : docs) {
                ret.add(new UserTreeNode(doc));
            }
        }

        return ret;
    }
}
