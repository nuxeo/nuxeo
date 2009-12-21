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
 * $Id: BlankImpl.java 19155 2007-05-22 16:19:48Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.api.impl;

import org.nuxeo.ecm.platform.relations.api.Blank;
import org.nuxeo.ecm.platform.relations.api.NodeType;

/**
 * Blank node.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class BlankImpl extends AbstractNode implements Blank {

    private static final long serialVersionUID = 1L;

    private String id;

    public BlankImpl() {
    }

    public BlankImpl(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public NodeType getNodeType() {
        return NodeType.BLANK;
    }

    @Override
    public boolean isBlank() {
        return true;
    }

    @Override
    public String toString() {
        String str;
        if (id != null) {
            str = String.format("<%s '%s'>", getClass(), id);
        } else {
            str = String.format("<%s>", getClass());
        }
        return str;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof BlankImpl)) {
            return false;
        }
        BlankImpl otherBlank = (BlankImpl) other;
        return id == null ? otherBlank.id == null
                : id.equals(otherBlank.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

}
