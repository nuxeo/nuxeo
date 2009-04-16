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
 * $$Id: Summary.java 28482 2008-01-04 15:33:39Z sfermigier $$
 */
package org.nuxeo.ecm.webapp.clipboard;

import java.util.List;
import java.util.Map;

/**
 * This class is used to build the summary file in a Zip export. It can displays
 * a summary in two ways:
 *
 * <ul>
 * <li>Flat with for all entries the full path to document
 * <li>Hierarchical.
 * </ul>
 *
 * This class extends Map&lt;String,SummaryEntry&gt; to store all entries and to
 * allows to store the same document many times, at different level in the
 * workingList. That's why the key is a String : the full path to the item in
 * the workingList, using documents UUID.
 *
 * @author <a href="mailto:bchaffangeon@nuxeo.com">Brice Chaffangeon</a>
 */
public interface Summary extends Map<String, SummaryEntry> {

    /**
     * Tests if the given entry has at least one child in the map.
     *
     * @param parentEntry is the entry to test
     * @return true if there is at least one child
     */
    boolean hasChild(SummaryEntry parentEntry);

    /**
     * @param parentEntry is parent of children you want to get
     * @return all the children of the parentEntry in the List
     */
    List<SummaryEntry> getChildren(SummaryEntry parentEntry);

    /**
     * Displays recursively on entry.
     *
     * @param sb is the String to display.
     * @param parentEntry is the entry to display
     * @return the String to display
     */
    String displayEntry(StringBuffer sb, SummaryEntry parentEntry);

    /**
     * Display all the map in a flat way. The display is ordered by path.
     *
     * @return the string to display
     */

    String toFlatList();

    /**
     * Displays all the map hierarchically.
     *
     * @return the string to display
     */

    String toTreeString();

    /**
     * Gets the root SummaryEntry in the map.
     *
     * @return the root SummaryEntry in the map
     */
    SummaryEntry getSummaryRoot();

}
