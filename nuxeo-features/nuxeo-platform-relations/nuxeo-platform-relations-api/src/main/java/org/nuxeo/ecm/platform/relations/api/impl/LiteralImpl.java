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
 * $Id: LiteralImpl.java 20796 2007-06-19 09:52:03Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.api.impl;

import org.nuxeo.ecm.platform.relations.api.Literal;
import org.nuxeo.ecm.platform.relations.api.NodeType;
import org.nuxeo.ecm.platform.relations.api.exceptions.InvalidLiteralException;

/**
 * Literal nodes.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class LiteralImpl extends AbstractNode implements Literal {

    private static final long serialVersionUID = 1L;

    protected String value;

    protected String language;

    protected String type;


    public LiteralImpl(String value) {
        // TODO: maybe handle encoding problems here
        this.value = value;
    }

    public NodeType getNodeType() {
        return NodeType.LITERAL;
    }

    @Override
    public boolean isLiteral() {
        return true;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        if (type != null) {
            throw new InvalidLiteralException(
                    "Cannot set language, type already set");
        }
        this.language = language;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (language != null) {
            throw new InvalidLiteralException(
                    "Cannot set type, language already set");
        }
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        String str;
        if (type != null) {
            str = String.format("<%s '%s^^%s'>", getClass(), value,
                    type);
        } else if (language != null) {
            str = String.format("<%s '%s@%s'>", getClass(), value,
                    language);
        } else {
            str = String.format("<%s '%s'>", getClass(), value);
        }
        return str;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LiteralImpl)) {
            return false;
        }
        LiteralImpl otherLiteral = (LiteralImpl) other;
        // XXX AT: will fail on different lit/language
        // boolean res = ((getLanguage() == otherLiteral.getLanguage())
        // && (getType() == otherLiteral.getType()) && (getValue()
        // .equals(otherLiteral.getValue())));
        boolean sameLanguage = language == null ? otherLiteral.language == null
                : language.equals(otherLiteral.language);
        boolean sameType = type == null ? otherLiteral.type == null
                : type.equals(otherLiteral.type);
        boolean sameValue = value == null ? otherLiteral.value == null
                : value.equals(otherLiteral.value);
        return sameLanguage && sameType && sameValue;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + (language == null ? 0 : language.hashCode());
        result = 37 * result + (type == null ? 0 : type.hashCode());
        result = 37 * result + (value == null ? 0 : value.hashCode());
        return result;
    }

}
