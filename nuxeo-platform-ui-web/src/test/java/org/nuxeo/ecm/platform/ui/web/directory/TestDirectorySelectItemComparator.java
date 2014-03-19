/*
 * (C) Copyright 2007-2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Thierry Martins
 *
 */

package org.nuxeo.ecm.platform.ui.web.directory;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.junit.Test;

public class TestDirectorySelectItemComparator {

    @Test
    public void testLocalizedItems() {
        List<DirectorySelectItem> items = new ArrayList<>();
        items.add(new DirectorySelectItem("tackle", "tacle", 0));
        items.add(new DirectorySelectItem("task", "tâche", 0));
        items.add(new DirectorySelectItem("mark", "tache", 0));

        // Check items will not be correctly sorted
        Collections.sort(items, new Comparator<DirectorySelectItem>() {

            @Override
            public int compare(DirectorySelectItem o1, DirectorySelectItem o2) {
                // simple ordering on label
                return o1.getLabel().compareTo(o2.getLabel());
            }
        });
        assertEquals("tache", items.get(0).getLabel());
        assertEquals("tacle", items.get(1).getLabel());
        assertEquals("tâche", items.get(2).getLabel());

        // Now items will be correctly sorted
        Collections.sort(items, new DirectorySelectItemComparator("label",
                false, new Locale("fr", "FR")));
        assertEquals("tache", items.get(0).getLabel());
        assertEquals("tâche", items.get(1).getLabel());
        assertEquals("tacle", items.get(2).getLabel());
    }

}