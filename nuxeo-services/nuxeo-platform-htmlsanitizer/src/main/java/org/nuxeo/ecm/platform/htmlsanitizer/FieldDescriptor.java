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
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.htmlsanitizer;

import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject(value = "field")
public class FieldDescriptor {

    @XContent
    protected String contentField;

    @XNode("@filter")
    protected String filterField;

    @XNode("@filterValue")
    protected String filterValue;

    @XNode("@sanitize")
    protected boolean sanitize = true;

    public String getContentField() {
        if (contentField != null) {
            String result = contentField.trim();
            result = result.replace("\n", "");
            return result;
        }
        return contentField;
    }

    public String getFilterField() {
        return filterField;
    }

    public String getFilterValue() {
        return filterValue;
    }

    public String[] getFilterValues() {
        return filterValue.split(",");
    }

    public boolean doSanitize() {
        return sanitize;
    }

    public boolean match(String fieldValue) {
        for (String v : getFilterValues()) {
            if (fieldValue.equals(v)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        if (filterField != null) {
            return getContentField() + " if " + filterField + (sanitize ? "=" : "!=") + filterValue;
        } else {
            return getContentField();
        }
    }

}
