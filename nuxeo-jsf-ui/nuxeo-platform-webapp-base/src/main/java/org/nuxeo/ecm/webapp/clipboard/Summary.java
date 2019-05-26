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
 * $$Id: Summary.java 28482 2008-01-04 15:33:39Z sfermigier $$
 */
package org.nuxeo.ecm.webapp.clipboard;

import java.util.List;
import java.util.Map;

/**
 * This class is used to build the summary file in a Zip export. It can displays a summary in two ways:
 * <ul>
 * <li>Flat with for all entries the full path to document
 * <li>Hierarchical.
 * </ul>
 * This class extends Map&lt;String,SummaryEntry&gt; to store all entries and to allows to store the same document many
 * times, at different level in the workingList. That's why the key is a String : the full path to the item in the
 * workingList, using documents UUID.
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
