/*
 * (C) Copyright 2010-2013 Nuxeo SA (http://nuxeo.com/) and others.
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.suggestbox.service.ComponentInitializationException;

@XObject("suggesterGroup")
public class SuggesterGroupDescriptor implements Cloneable {

    private static final Log log = LogFactory.getLog(SuggesterGroupDescriptor.class);

    @XNode("@name")
    protected String name = "default";

    @XNodeList(value = "suggesters/suggesterName", type = ArrayList.class, componentType = SuggesterGroupItemDescriptor.class)
    List<SuggesterGroupItemDescriptor> suggesters;

    public String getName() {
        return name;
    }

    public List<SuggesterGroupItemDescriptor> getSuggesters() {
        return suggesters;
    }

    public void mergeFrom(SuggesterGroupDescriptor newDescriptor) throws ComponentInitializationException {
        if (name == null || !name.equals(newDescriptor.name)) {
            throw new RuntimeException("Cannot merge descriptor with name '" + name
                    + "' with another descriptor with different name " + newDescriptor.getName() + "'");
        }
        log.info(String.format("Merging suggester group '%s'.", name));
        // merge the suggesterNames
        for (SuggesterGroupItemDescriptor newSuggesterGroupItem : newDescriptor.getSuggesters()) {
            String newSuggesterName = newSuggesterGroupItem.getName();
            // manage remove
            if (newSuggesterGroupItem.isRemove()) {
                boolean isSuggesterRemoved = remove(newSuggesterName);
                if (!isSuggesterRemoved) {
                    log.warn(String.format(
                            "Cannot remove suggester '%s' because it does not exist in suggesterGroup '%s'.",
                            newSuggesterName, name));
                }
            }
            // manage appendBefore, appendAfter or no particular attributes
            else {
                String appendBeforeSuggesterName = newSuggesterGroupItem.getAppendBefore();
                String appendAfterSuggesterName = newSuggesterGroupItem.getAppendAfter();
                // can't have both appendBefore and appendAfter
                if (appendBeforeSuggesterName != null && appendAfterSuggesterName != null) {
                    throw new RuntimeException(String.format(
                            "Cannot define both 'appendBefore' and 'appendAfter' attributes on suggester '%s'.",
                            newSuggesterName));
                }
                // manage appendBefore
                if (appendBeforeSuggesterName != null) {
                    boolean isSuggesterAppended = appendBefore(appendBeforeSuggesterName, newSuggesterName);
                    if (!isSuggesterAppended) {
                        logExistingSuggesterName(newSuggesterName);
                    }
                }
                // manage appendAfter
                else if (appendAfterSuggesterName != null) {
                    boolean isSuggesterAppended = appendAfter(appendAfterSuggesterName, newSuggesterName);
                    if (!isSuggesterAppended) {
                        logExistingSuggesterName(newSuggesterName);
                    }
                }
                // manage the case of no particular attributes => append
                // suggester at the end of the list
                else if (appendBeforeSuggesterName == null && appendAfterSuggesterName == null) {
                    boolean isSuggesterAppended = appendAfter(null, newSuggesterName);
                    if (!isSuggesterAppended) {
                        logExistingSuggesterName(newSuggesterName);
                    }
                }
            }
        }
    }

    /*
     * Override the Object.clone to make it public
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    /**
     * Removes the suggester named {@code suggesterName} from the {@code #suggesters} list.
     *
     * @param suggesterName the suggester name
     * @return true, if a suggester was removed
     */
    protected boolean remove(String suggesterName) {
        Iterator<SuggesterGroupItemDescriptor> suggestersIt = suggesters.iterator();
        while (suggestersIt.hasNext()) {
            SuggesterGroupItemDescriptor suggesterGroupItem = suggestersIt.next();
            if (suggesterName.equals(suggesterGroupItem.getName())) {
                suggestersIt.remove();
                log.debug(String.format("Removed suggester '%s' from suggesterGroup '%s'.", suggesterName, name));
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the index of the first occurrence of the element named {@code suggesterName} in the {@code #suggesters}
     * list, or -1 if {@code suggesterName} is null or if this list does not contain the element.
     *
     * @param suggesterName the suggester name
     * @return the index of the first occurrence of the element named {@code suggesterName} in the {@code #suggesters}
     *         list, or -1 if {@code suggesterName} is null or if this list does not contain the element
     */
    protected int indexOf(String suggesterName) {
        if (suggesterName != null) {
            int index = 0;
            Iterator<SuggesterGroupItemDescriptor> suggestersIt = suggesters.iterator();
            while (suggestersIt.hasNext()) {
                SuggesterGroupItemDescriptor suggesterGroupItem = suggestersIt.next();
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
     * @param suggesterName the suggester name
     * @param newSuggesterName the name of the suggester to append
     * @return true, if the suggester named {@code newSuggesterName} was appended to the {@code #suggesters} list
     */
    protected boolean appendBefore(String suggesterName, String newSuggesterName) {
        return append(suggesterName, newSuggesterName, true);
    }

    /**
     * Unless a suggester named {@code newSuggesterName} already exists in the {@code #suggesters} list, appends a new
     * {@code SuggesterGroupItemDescriptor} named {@code newSuggesterName} just after the suggester named
     * {@code suggesterName} in the {@code #suggesters} list. If the suggester named {@code suggesterName} does not
     * exist, appends the new suggester at the end of the list.
     *
     * @param suggesterName the suggester name
     * @param newSuggesterName the name of the suggester to append
     * @return true, if the suggester named {@code newSuggesterName} was appended to the {@code #suggesters} list
     */
    protected boolean appendAfter(String suggesterName, String newSuggesterName) {
        return append(suggesterName, newSuggesterName, false);
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
    protected boolean append(String suggesterName, String newSuggesterName, boolean before) {
        // check if the new suggester's name doesn't already exist in the
        // suggesters list
        if (indexOf(newSuggesterName) > -1) {
            return false;
        }
        // new suggester
        SuggesterGroupItemDescriptor newSuggester = new SuggesterGroupItemDescriptor(newSuggesterName);
        int indexOfSuggester = indexOf(suggesterName);
        if (indexOfSuggester > -1) {
            // suggester found, append new suggester before or after it
            int indexOfNewSuggester = before ? indexOfSuggester : indexOfSuggester + 1;
            suggesters.add(indexOfNewSuggester, newSuggester);
            log.debug(String.format("Appended suggester '%s' %s suggester '%s' in suggesterGroup '%s'.",
                    newSuggesterName, before ? "before" : "after", suggesterName, name));
        } else {
            // suggester not found, append new suggester at the beginning or the
            // end of the suggesters list
            if (before) {
                suggesters.add(0, newSuggester);
                if (suggesterName != null) {
                    log.warn(String.format(
                            "Could not append suggester '%s' before suggester '%s' in suggesterGroup '%s' because '%s' does not exist in this suggesterGroup. Appended it before all suggesters.",
                            newSuggesterName, suggesterName, name, suggesterName));
                }
            } else {
                suggesters.add(newSuggester);
                if (suggesterName != null) {
                    log.warn(String.format(
                            "Could not append suggester '%s' after suggester '%s' in suggesterGroup '%s' because '%s' does not exist in this suggesterGroup. Appended it after all suggesters.",
                            newSuggesterName, suggesterName, name, suggesterName));
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
        log.warn(String.format(
                "Suggester '%s' already exists in suggesterGroup '%s'. Cannot have two occurrences of the same suggester, so won't append it.",
                newSuggesterName, name));
    }
}
