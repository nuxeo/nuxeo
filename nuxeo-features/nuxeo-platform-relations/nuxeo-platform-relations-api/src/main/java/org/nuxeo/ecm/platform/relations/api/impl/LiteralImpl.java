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

    @Override
    public NodeType getNodeType() {
        return NodeType.LITERAL;
    }

    @Override
    public boolean isLiteral() {
        return true;
    }

    @Override
    public String getLanguage() {
        return language;
    }

    @Override
    public void setLanguage(String language) {
        if (type != null) {
            throw new InvalidLiteralException("Cannot set language, type already set");
        }
        this.language = language;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        if (language != null) {
            throw new InvalidLiteralException("Cannot set type, language already set");
        }
        this.type = type;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        String str;
        if (type != null) {
            str = String.format("%s('%s^^%s')", getClass().getSimpleName(), value, type);
        } else if (language != null) {
            str = String.format("%s('%s@%s')", getClass().getSimpleName(), value, language);
        } else {
            str = String.format("%s('%s')", getClass().getSimpleName(), value);
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
        boolean sameType = type == null ? otherLiteral.type == null : type.equals(otherLiteral.type);
        boolean sameValue = value == null ? otherLiteral.value == null : value.equals(otherLiteral.value);
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
