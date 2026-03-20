/*
 * (C) Copyright 2010-2026 Nuxeo (http://nuxeo.com/) and others.
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
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.suggestbox.service.descriptors;

import static org.apache.commons.lang3.ObjectUtils.getIfNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.runtime.model.Descriptor;

@XObject("suggesterGroup")
public class SuggesterGroupDescriptor implements Descriptor {

    private static final Logger log = LogManager.getLogger(SuggesterGroupDescriptor.class);

    @XNode("@name")
    protected String name = "default";

    @XNodeList(value = "suggesters/suggesterName", type = ArrayList.class, componentType = SuggesterGroupItemDescriptor.class)
    protected List<SuggesterGroupItemDescriptor> suggesters;

    @Override
    public String getId() {
        return name;
    }

    public String getName() {
        return name;
    }

    public List<SuggesterGroupItemDescriptor> getSuggesters() {
        return suggesters;
    }

    @Override
    public Descriptor merge(Descriptor o) {
        var other = (SuggesterGroupDescriptor) o;
        var merged = new SuggesterGroupDescriptor();
        log.info("Merging suggester group: {}", name);
        merged.name = getIfNull(other.name, name);
        // merge the suggesters
        merged.suggesters = new ArrayList<>(suggesters);
        for (var otherSuggesters : other.getSuggesters()) {
            String otherSuggesterName = otherSuggesters.getName();
            // manage remove
            if (otherSuggesters.isRemove()) {
                boolean isSuggesterRemoved = remove(merged, otherSuggesterName);
                if (!isSuggesterRemoved) {
                    log.warn("Cannot remove suggester: {} because it does not exist in suggesterGroup: {}",
                            otherSuggesterName, name);
                }
            }
            // manage appendBefore, appendAfter or no particular attributes
            else {
                String appendBeforeSuggesterName = otherSuggesters.getAppendBefore();
                String appendAfterSuggesterName = otherSuggesters.getAppendAfter();
                // can't have both appendBefore and appendAfter
                if (appendBeforeSuggesterName != null && appendAfterSuggesterName != null) {
                    throw new NuxeoException(String.format(
                            "Cannot define both 'appendBefore' and 'appendAfter' attributes on suggester: %s.",
                            otherSuggesterName));
                }
                // manage appendBefore
                if (appendBeforeSuggesterName != null) {
                    boolean isSuggesterAppended = appendBefore(merged, appendBeforeSuggesterName, otherSuggesterName);
                    if (!isSuggesterAppended) {
                        logExistingSuggesterName(otherSuggesterName);
                    }
                }
                // manage appendAfter
                else if (appendAfterSuggesterName != null) {
                    boolean isSuggesterAppended = appendAfter(merged, appendAfterSuggesterName, otherSuggesterName);
                    if (!isSuggesterAppended) {
                        logExistingSuggesterName(otherSuggesterName);
                    }
                }
                // manage the case of no particular attributes => append suggester at the end of the list
                else {
                    boolean isSuggesterAppended = appendAfter(merged, null, otherSuggesterName);
                    if (!isSuggesterAppended) {
                        logExistingSuggesterName(otherSuggesterName);
                    }
                }
            }
        }
        return merged;
    }

    /**
     * Removes the suggester named {@code suggesterName} from the {@code #suggesters} list.
     *
     * @param merged the descriptor to merged into
     * @param suggesterName the suggester name
     * @return true, if a suggester was removed
     */
    protected static boolean remove(SuggesterGroupDescriptor merged, String suggesterName) {
        Iterator<SuggesterGroupItemDescriptor> suggestersIt = merged.suggesters.iterator();
        while (suggestersIt.hasNext()) {
            SuggesterGroupItemDescriptor suggesterGroupItem = suggestersIt.next();
            if (suggesterName.equals(suggesterGroupItem.getName())) {
                suggestersIt.remove();
                log.debug("Removed suggester: {} from suggesterGroup: {}", suggesterName, merged.name);
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the index of the first occurrence of the element named {@code suggesterName} in the {@code #suggesters}
     * list, or -1 if {@code suggesterName} is null or if this list does not contain the element.
     *
     * @param merged the descriptor to merged into
     * @param suggesterName the suggester name
     * @return the index of the first occurrence of the element named {@code suggesterName} in the {@code #suggesters}
     *         list, or -1 if {@code suggesterName} is null or if this list does not contain the element
     */
    protected static int indexOf(SuggesterGroupDescriptor merged, String suggesterName) {
        if (suggesterName != null) {
            int index = 0;
            for (SuggesterGroupItemDescriptor suggesterGroupItem : merged.suggesters) {
                if (suggesterName.equals(suggesterGroupItem.getName())) {
                    return index;
                }
                index++;
            }
        }
        return -1;
    }

    /**
     * Unless a suggester named {@code newSuggesterName} already exists in the {@code #suggesters} list, appends a new
     * {@code SuggesterGroupItemDescriptor} named {@code newSuggesterName} just before the suggester named
     * {@code suggesterName} in the {@code #suggesters} list. If the suggester named {@code suggesterName} does not
     * exist, appends the new suggester at the beginning of the list.
     *
     * @param merged the descriptor to merged into
     * @param suggesterName the suggester name
     * @param newSuggesterName the name of the suggester to append
     * @return true, if the suggester named {@code newSuggesterName} was appended to the {@code #suggesters} list
     */
    protected static boolean appendBefore(SuggesterGroupDescriptor merged, String suggesterName,
            String newSuggesterName) {
        return append(merged, suggesterName, newSuggesterName, true);
    }

    /**
     * Unless a suggester named {@code newSuggesterName} already exists in the {@code #suggesters} list, appends a new
     * {@code SuggesterGroupItemDescriptor} named {@code newSuggesterName} just after the suggester named
     * {@code suggesterName} in the {@code #suggesters} list. If the suggester named {@code suggesterName} does not
     * exist, appends the new suggester at the end of the list.
     *
     * @param merged the descriptor to merged into
     * @param suggesterName the suggester name
     * @param newSuggesterName the name of the suggester to append
     * @return true, if the suggester named {@code newSuggesterName} was appended to the {@code #suggesters} list
     */
    protected static boolean appendAfter(SuggesterGroupDescriptor merged, String suggesterName,
            String newSuggesterName) {
        return append(merged, suggesterName, newSuggesterName, false);
    }

    /**
     * Unless a suggester named {@code newSuggesterName} already exists in the {@code #suggesters} list, appends a new
     * {@code SuggesterGroupItemDescriptor} named {@code newSuggesterName} just before (if {@code before} is true) or
     * after the suggester named {@code suggesterName} in the {@code #suggesters} list. If the suggester named
     * {@code suggesterName} does not exist, appends the new suggester at the beginning or the end of the list,
     * depending on {@code before}.
     *
     * @param suggesterName the suggester name
     * @param newSuggesterName the name of the suggester to append
     * @return true, if the suggester named {@code newSuggesterName} was appended to the {@code #suggesters} list
     */
    protected static boolean append(SuggesterGroupDescriptor merged, String suggesterName, String newSuggesterName,
            boolean before) {
        // check if the new suggester's name doesn't already exist in the suggesters list
        if (indexOf(merged, newSuggesterName) > -1) {
            return false;
        }
        // new suggester
        SuggesterGroupItemDescriptor newSuggester = new SuggesterGroupItemDescriptor(newSuggesterName);
        int indexOfSuggester = indexOf(merged, suggesterName);
        if (indexOfSuggester > -1) {
            // suggester found, append new suggester before or after it
            int indexOfNewSuggester = before ? indexOfSuggester : indexOfSuggester + 1;
            merged.suggesters.add(indexOfNewSuggester, newSuggester);
            log.debug("Appended suggester: {} {} suggester: {} in suggesterGroup: {}", newSuggesterName,
                    before ? "before" : "after", suggesterName, merged.name);
        } else {
            // suggester not found, append new suggester at the beginning or the end of the suggesters list
            if (before) {
                merged.suggesters.addFirst(newSuggester);
                if (suggesterName != null) {
                    log.warn(
                            "Could not append suggester: {} before suggester: {} in suggesterGroup: {} because: {} does not exist in this suggesterGroup. Appended it before all suggesters.",
                            newSuggesterName, suggesterName, merged.name, suggesterName);
                }
            } else {
                merged.suggesters.addLast(newSuggester);
                if (suggesterName != null) {
                    log.warn(
                            "Could not append suggester: {} after suggester: {} in suggesterGroup: {} because: {} does not exist in this suggesterGroup. Appended it after all suggesters.",
                            newSuggesterName, suggesterName, merged.name, suggesterName);
                }
            }
        }
        return true;
    }

    /**
     * Logs that the suggester named {@code newSuggesterName} already exists in the {@code #suggesters} list and
     * therefore won't be appended to it.
     *
     * @param newSuggesterName the new suggester name
     */
    protected void logExistingSuggesterName(String newSuggesterName) {
        log.warn(
                "Suggester: {} already exists in suggesterGroup: {}. Cannot have two occurrences of the same suggester, so won't append it.",
                newSuggesterName, name);
    }
}
