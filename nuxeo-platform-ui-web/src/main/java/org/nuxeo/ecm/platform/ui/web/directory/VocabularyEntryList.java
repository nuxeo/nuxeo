/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.directory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 *
 */
public class VocabularyEntryList implements Serializable {

    private static final long serialVersionUID = 7342526066954918526L;

    private final List<VocabularyEntry> entries;
    private final String name;

    public VocabularyEntryList(String name, List<VocabularyEntry> entries) {
        this.name = name;
        this.entries = entries;
    }

    public VocabularyEntryList(String name) {
        this.name = name;
        entries = new ArrayList<VocabularyEntry>();
    }

    public String getName() {
        return name;
    }

    public void add(VocabularyEntry entry) {
        entries.add(entry);
    }

    public List<VocabularyEntry> getEntries() {
        return entries;
    }

    public List<VocabularyEntry> getEntries(String parent) {
        List<VocabularyEntry> result = new ArrayList<VocabularyEntry>();
        for (VocabularyEntry entry : entries) {
            if (parent.equals(entry.getParent())) {
                result.add(entry);
            }
        }
        return result;
    }
}
