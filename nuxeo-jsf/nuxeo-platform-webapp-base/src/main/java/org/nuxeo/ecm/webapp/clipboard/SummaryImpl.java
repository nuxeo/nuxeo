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
 * This class is an implementation of the interface Summary. It intends to build and display a summary, thanks to
 * summary items in the HashMap.
 *
 * @author <a href="bchaffangeon@nuxeo.com">Brice Chaffangeon</a>
 */
public class SummaryImpl extends HashMap<String, SummaryEntry> implements Summary, Serializable {

    private static final long serialVersionUID = -7791997708511426604L;

    @Override
    public boolean hasChild(SummaryEntry parentEntry) {
        Set<String> key = keySet();

        for (String pathRef : key) {
            SummaryEntry summaryEntry = get(pathRef);
            if (summaryEntry.getParent() != null && summaryEntry.getParent().equals(parentEntry)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<SummaryEntry> getChildren(SummaryEntry parentEntry) {
        List<SummaryEntry> children = null;
        Set<String> key = keySet();

        for (String pathRef : key) {
            SummaryEntry summaryEntry = get(pathRef);

            if (summaryEntry.getParent() != null && summaryEntry.getParent().equals(parentEntry)) {
                if (children == null) {
                    children = new ArrayList<>();
                }
                children.add(get(pathRef));
            }
        }
        return children;
    }

    @Override
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
                    child.setMarker(parentEntry.getMarker() + child.getMarker() + child.getMarker());
                    displayEntry(sb, child);
                }
            }
        }
        return sb.toString();
    }

    @Override
    public String toFlatList() {
        StringBuilder sb = new StringBuilder();
        List<SummaryEntry> entryList = new ArrayList<>();
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

    @Override
    public String toTreeString() {
        return displayEntry(null, getSummaryRoot());
    }

    /**
     * Gets the root SummaryEntry in the map, usually identified by a key in the map set to 0.
     *
     * @return the root SummaryEntry in the map
     */
    @Override
    public SummaryEntry getSummaryRoot() {
        return get(new IdRef("0").toString());
    }

}
