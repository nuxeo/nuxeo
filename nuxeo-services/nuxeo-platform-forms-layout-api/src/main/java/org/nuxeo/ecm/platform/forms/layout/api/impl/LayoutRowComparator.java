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
import java.util.List;

import org.nuxeo.ecm.platform.forms.layout.api.LayoutRow;

/**
 * Compares selected row instances to order them in the same order that given by the input list of row names.
 *
 * @author Anahide Tchertchian
 */
public class LayoutRowComparator implements Comparator<LayoutRow> {

    protected List<String> rowNames;

    public LayoutRowComparator(List<String> rowNames) {
        super();
        this.rowNames = rowNames;
    }

    @Override
    public int compare(LayoutRow o1, LayoutRow o2) {
        if (rowNames == null) {
            return 0;
        }
        Integer index1 = Integer.valueOf(rowNames.indexOf(o1.getName()));
        Integer index2 = Integer.valueOf(rowNames.indexOf(o2.getName()));
        return index1.compareTo(index2);
    }
}
