/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: AbstractNode.java 20645 2007-06-17 13:16:54Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.api.impl;

import org.nuxeo.ecm.platform.relations.api.Node;

/**
 * Abstract class for nodes.
 * <p>
 * Nodes can be resources, blank nodes or literals.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public abstract class AbstractNode implements Node {

    private static final long serialVersionUID = 1L;

    public boolean isLiteral() {
        return false;
    }

    public boolean isBlank() {
        return false;
    }

    public boolean isResource() {
        return false;
    }

    public boolean isQNameResource() {
        return false;
    }

    public int compareTo(Node o) {
        // dummy override, just used to compare statements lists
        return toString().compareTo(o.toString());
    }

}
