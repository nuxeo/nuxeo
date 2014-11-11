/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.export.io;

import java.util.Comparator;

import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeConfiguration;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetTypeDefinition;

/**
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class WidgetTypeDefinitionComparator implements
        Comparator<WidgetTypeDefinition> {

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
        return getLabel(o1).compareTo(getLabel(o2));
    }

    protected String getLabel(WidgetTypeDefinition def) {
        String res = def.getName();
        WidgetTypeConfiguration conf = def.getConfiguration();
        if (conf != null && conf.getTitle() != null) {
            res = conf.getTitle();
        }
        return res;
    }

}
