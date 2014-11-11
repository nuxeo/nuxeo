/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     ${user}
 *
 * $$Id: SummaryImpl.java 28482 2008-01-04 15:33:39Z sfermigier $$
 */
package org.nuxeo.ecm.webapp.clipboard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.nuxeo.ecm.core.api.IdRef;

/**
 * This class is an implementation of the interface Summary. It intends to build
 * and display a summary, thanks to summary items in the HashMap.
 *
 * @author <a href="bchaffangeon@nuxeo.com">Brice Chaffangeon</a>
 */
public class SummaryImpl extends HashMap<String, SummaryEntry> implements
        Summary, Serializable {

    private static final long serialVersionUID = -7791997708511426604L;

    public boolean hasChild(SummaryEntry parentEntry) {
        Set<String> key = keySet();

        for (String pathRef : key) {
            SummaryEntry summaryEntry = get(pathRef);
            if (summaryEntry.getParent() != null
                    && summaryEntry.getParent().equals(parentEntry)) {
                return true;
            }
        }
        return false;
    }

    public List<SummaryEntry> getChildren(SummaryEntry parentEntry) {
        List<SummaryEntry> children = null;
        Set<String> key = keySet();

        for (String pathRef : key) {
            SummaryEntry summaryEntry = get(pathRef);

            if (summaryEntry.getParent() != null
                    && summaryEntry.getParent().equals(parentEntry)) {
                if (children == null) {
                    children = new ArrayList<SummaryEntry>();
                }
                children.add(get(pathRef));
            }
        }
        return children;
    }

    public String displayEntry(StringBuffer sb, SummaryEntry parentEntry) {
        if (sb == null) {
            sb = new StringBuffer();
        }
        if (parentEntry != null) {
            if (hasChild(parentEntry)) {
                parentEntry.setBullet("+ ");
            }
            sb.append(parentEntry.toTreeString());
            List<SummaryEntry> childrens = getChildren(parentEntry);
            if (childrens != null && !childrens.isEmpty()) {
                for (SummaryEntry child : childrens) {
                    // Force the marker to increment the blank space for a child
                    child.setMarker(parentEntry.getMarker() + child.getMarker()
                            + child.getMarker());
                    displayEntry(sb, child);
                }
            }
        }
        return sb.toString();
    }

    public String toFlatList() {
        StringBuilder sb = new StringBuilder();
        List<SummaryEntry> entryList = new ArrayList<SummaryEntry>();
        Set<String> key = keySet();
        for (String docRef : key) {
            entryList.add(get(docRef));
        }
        Collections.sort(entryList);

        for (SummaryEntry summaryEntry : entryList) {
            sb.append(summaryEntry.toFlatString());
        }
        return sb.toString();
    }

    /**
     * @return the hierarchical view by default.
     */
    @Override
    public String toString() {
        return toTreeString();
    }

    public String toTreeString() {
        return displayEntry(null, getSummaryRoot());
    }

    /**
     * Gets the root SummaryEntry in the map, usually identified by
     * a key in the map set to 0.
     *
     * @return the root SummaryEntry in the map
     */
    public SummaryEntry getSummaryRoot() {
        return get(new IdRef("0").toString());
    }

}
