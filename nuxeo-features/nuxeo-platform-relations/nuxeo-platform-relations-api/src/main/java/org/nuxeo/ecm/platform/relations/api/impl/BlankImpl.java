/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
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
            str = String.format("%s('%s')", getClass().getSimpleName(), id);
        } else {
            str = String.format("%s()", getClass().getSimpleName());
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
        return id == null ? otherBlank.id == null : id.equals(otherBlank.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }

}
