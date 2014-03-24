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

    @SuppressWarnings("unchecked")
    @Test
    public void testLocalizedItems() {
        ArrayList<DirectorySelectItem> items = new ArrayList<DirectorySelectItem>();
        List<DirectorySelectItem> dupItems;
        items.add(new DirectorySelectItem("mark0", "Tache", 0));
        items.add(new DirectorySelectItem("tackle", "tacle", 0));
        items.add(new DirectorySelectItem("task", "tâche", 0));
        items.add(new DirectorySelectItem("mark", "tache", 0));
        items.add(new DirectorySelectItem("mark1", "Tache", 0));

        // Check items will not be correctly sorted
        dupItems = (ArrayList<DirectorySelectItem>) items.clone();
        Collections.sort(dupItems, new Comparator<DirectorySelectItem>() {

            @Override
            public int compare(DirectorySelectItem o1, DirectorySelectItem o2) {
                // simple ordering on label
                return o1.getLabel().compareTo(o2.getLabel());
            }
        });
        assertEquals("Tache", dupItems.get(0).getLabel());
        assertEquals("Tache", dupItems.get(1).getLabel());
        assertEquals("tache", dupItems.get(2).getLabel());
        assertEquals("tacle", dupItems.get(3).getLabel());
        assertEquals("tâche", dupItems.get(4).getLabel());

        // Items will be correctly sorted, but without looking at the case
        dupItems = (ArrayList<DirectorySelectItem>) items.clone();
        Collections.sort(dupItems, new DirectorySelectItemComparator("label",
                false, new Locale("fr", "FR")));
        assertEquals("Tache", dupItems.get(0).getLabel());
        assertEquals("tache", dupItems.get(1).getLabel()); // this item is
                                                           // between the other
                                                           // 'Tache' because of
                                                           // case insensitive
                                                           // sort
        assertEquals("Tache", dupItems.get(2).getLabel());
        assertEquals("tâche", dupItems.get(3).getLabel()); // this item is now
                                                           // correctly sorted
        assertEquals("tacle", dupItems.get(4).getLabel());

        // Now items will be correctly sorted, taking into account the case and
        // the accents as expected
        Collections.sort(items, new DirectorySelectItemComparator("label",
                true, new Locale("fr", "FR")));
        assertEquals("tache", items.get(0).getLabel());
        assertEquals("Tache", items.get(1).getLabel());
        assertEquals("Tache", items.get(2).getLabel());
        assertEquals("tâche", items.get(3).getLabel());
        assertEquals("tacle", items.get(4).getLabel());
    }

}