/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Dragos Mihalache
 *     Florent Guillaume
 */

package org.nuxeo.ecm.platform.versioning.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This class composes a result of versioning interrogation about what increment options are available.
 *
 * @see org.nuxeo.ecm.platform.versioning.api.VersioningActions
 */
public class VersionIncEditOptions implements Serializable {

    private static final long serialVersionUID = 1L;

    private VersioningActions defaultVersioningAction;

    private final List<VersioningActions> options = new ArrayList<>();

    /**
     * Returns action to be presented by default to user.
     * <p>
     * Since 5.7.3, returns {@link VersioningActions#ACTION_NO_INCREMENT} by default instead of null, when not set.
     */
    public VersioningActions getDefaultVersioningAction() {
        if (defaultVersioningAction == null) {
            return VersioningActions.ACTION_NO_INCREMENT;
        }
        return defaultVersioningAction;
    }

    public void setDefaultVersioningAction(VersioningActions defaultVersioningAction) {
        this.defaultVersioningAction = defaultVersioningAction;
    }

    public void addOption(VersioningActions option) {
        options.add(option);
    }

    public List<VersioningActions> getOptions() {
        return options;
    }

    /**
     * Returns true if some incrementation options are defined.
     *
     * @since 5.7.3
     */
    public boolean hasOptions() {
        return options != null && !options.isEmpty();
    }

    /**
     * Returns the versioning selection options for display.
     *
     * @since 5.7.3
     */
    public Map<String, String> getOptionsForDisplay() {
        Map<String, String> map = new LinkedHashMap<>();
        if (options != null) {
            for (VersioningActions option : options) {
                String label = "label.versioning.option." + option.toString();
                map.put(option.name(), label);
            }
        }
        return map;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + options + ')';
    }

}
