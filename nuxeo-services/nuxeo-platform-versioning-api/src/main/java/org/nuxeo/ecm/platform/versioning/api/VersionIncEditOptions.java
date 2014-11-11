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
import java.util.List;

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
     */
    public VersioningActions getDefaultVersioningAction() {
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

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + options + ')';
    }

}
