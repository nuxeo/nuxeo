/*
 * (C) Copyright 2015-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 */

package org.nuxeo.ecm.core.query.sql.model;

import org.apache.commons.lang3.StringUtils;

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
