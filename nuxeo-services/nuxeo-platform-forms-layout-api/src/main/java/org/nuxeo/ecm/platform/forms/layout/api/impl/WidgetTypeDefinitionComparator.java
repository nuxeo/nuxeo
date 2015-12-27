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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.api.impl;

import java.util.Comparator;

import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeConfiguration;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;

/**
 * Compares widget types on id or label.
 *
 * @author Anahide Tchertchian
 * @since 5.4.2
 */
public class WidgetTypeDefinitionComparator implements Comparator<WidgetTypeDefinition> {

    protected boolean compareLabels = false;

    public WidgetTypeDefinitionComparator(boolean compareLabels) {
        super();
        this.compareLabels = compareLabels;
    }

    @Override
    public int compare(WidgetTypeDefinition o1, WidgetTypeDefinition o2) {
        if (o1 == null && o2 == null) {
            return 0;
        }
        if (o1 == null) {
            return -1;
        }
        if (o2 == null) {
            return 1;
        }
        return getComparisonString(o1).compareTo(getComparisonString(o2));
    }

    protected String getComparisonString(WidgetTypeDefinition def) {
        String res = def.getName();
        if (compareLabels) {
            WidgetTypeConfiguration conf = def.getConfiguration();
            if (conf != null && conf.getTitle() != null) {
                res = conf.getTitle();
            }
        }
        return res;
    }

}
