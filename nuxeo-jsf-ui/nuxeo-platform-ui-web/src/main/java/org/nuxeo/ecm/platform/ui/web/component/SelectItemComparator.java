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
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.component;

import java.io.Serializable;
import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.commons.lang3.StringUtils;

/**
 * Orders select items by id or label, handling localization.
 *
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class SelectItemComparator implements Comparator<SelectItem>, Serializable {

    private static final long serialVersionUID = 1L;

    private final String[] ordering;

    private final Boolean caseSensitive;

    private final Collator collator;

    private final Locale locale;

    public SelectItemComparator(String ordering, Boolean caseSentitive, Locale locale) {
        this.ordering = StringUtils.split(ordering, ",");
        caseSensitive = caseSentitive;
        if (locale == null) {
            FacesContext context = FacesContext.getCurrentInstance();
            this.locale = context.getViewRoot().getLocale();
        } else {
            this.locale = locale;
        }
        collator = Collator.getInstance(this.locale);
        if (Boolean.TRUE.equals(caseSensitive)) {
            collator.setStrength(Collator.TERTIARY); // TERTIARY will make a
                                                     // difference between 'a'
                                                     // and 'A'
        } else {
            collator.setStrength(Collator.SECONDARY);
        }
    }

    public SelectItemComparator(String ordering, Boolean caseSensitive) {
        this(ordering, caseSensitive, null);
    }

    protected int compareField(String field, SelectItem item1, SelectItem item2) {
        if ("label".equals(field)) {
            String label1 = item1.getLabel();
            String label2 = item2.getLabel();
            return collator.compare(label1, label2);
        } else if ("id".equals(field)) {
            String value1 = String.valueOf(item1.getValue());
            String value2 = String.valueOf(item2.getValue());
            // TODO: maybe deal with numbers comparisons (?)
            return collator.compare(value1, value2);
        } else {
            throw new RuntimeException("Invalid sort criteria " + field);
        }
    }

    @Override
    public int compare(SelectItem item1, SelectItem item2) {
        for (String field : ordering) {
            int compare = compareField(field, item1, item2);
            if (compare != 0) {
                return compare;
            }
        }
        return 0;
    }

}
