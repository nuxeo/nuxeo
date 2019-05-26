/*
 * (C) Copyright 2007-2014 Nuxeo SA (http://nuxeo.com/) and others.
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
        ArrayList<DirectorySelectItem> items = new ArrayList<>();
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
        Collections.sort(dupItems, new DirectorySelectItemComparator("label", false, new Locale("fr", "FR")));
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
        Collections.sort(items, new DirectorySelectItemComparator("label", true, new Locale("fr", "FR")));
        assertEquals("tache", items.get(0).getLabel());
        assertEquals("Tache", items.get(1).getLabel());
        assertEquals("Tache", items.get(2).getLabel());
        assertEquals("tâche", items.get(3).getLabel());
        assertEquals("tacle", items.get(4).getLabel());
    }

}
