/*
 * (C) Copyright 2006-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 * This class composes a result of versioning interrogation about what
 * increment options are available.
 *
 * @see org.nuxeo.ecm.platform.versioning.api.VersioningActions
 */
public class VersionIncEditOptions implements Serializable {

    private static final long serialVersionUID = 1L;

    private VersioningActions defaultVersioningAction;

    private final List<VersioningActions> options = new ArrayList<VersioningActions>();

    /**
     * Returns action to be presented by default to user.
     * <p>
     * Since 5.7.3, returns {@link VersioningActions#ACTION_NO_INCREMENT} by
     * default instead of null, when not set.
     */
    public VersioningActions getDefaultVersioningAction() {
        if (defaultVersioningAction == null) {
            return VersioningActions.ACTION_NO_INCREMENT;
        }
        return defaultVersioningAction;
    }

    public void setDefaultVersioningAction(
            VersioningActions defaultVersioningAction) {
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
        Map<String, String> map = new LinkedHashMap<String, String>();
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
