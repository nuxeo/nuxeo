/*
 * Copyright (c) 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Benoit Delbosc
 */

package org.nuxeo.ecm.core.query.sql.model;

import org.apache.commons.lang.StringUtils;

public class EsHint implements Operand {

    private static final long serialVersionUID = 4590329982296853715L;

    public final String index;

    public final String analyzer;

    public final String operator;

    public EsHint(EsIdentifierList index, String analyzer, String operator) {
        this.index = (index == null) ? null : index.toString();
        this.analyzer = analyzer;
        this.operator = operator;
    }

    @Override
    public void accept(IVisitor visitor) {
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("/*+ES: ");
        if (index != null) {
            buf.append(String.format("INDEX(%s) ", index));
        }
        if (analyzer != null) {
            buf.append(String.format("ANALYZER(%s) ", analyzer));
        }
        if (operator != null) {
            buf.append(String.format("OPERATOR(%s) ", operator));
        }
        buf.append("*/");
        return buf.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof EsHint)) {
            return false;
        }
        return equals((EsHint) obj);
    }

    private boolean equals(EsHint other) {
        return StringUtils.equals(index, other.index) && StringUtils.equals(analyzer, other.analyzer)
                && StringUtils.equals(operator, other.operator);
    }

    public String[] getIndex() {
        if (index == null) {
            return null;
        }
        return index.split(",");
    }
}
