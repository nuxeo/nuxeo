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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.versioning.api;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class composes a result of versioning interrogation about what
 * incrementation options are available. If the versioningAction is
 * ACTION_CASE_DEPENDENT then options should be presented to the user.
 *
 * @see org.nuxeo.ecm.platform.versioning.api.VersioningActions
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class VersionIncEditOptions implements Serializable {

    private static final long serialVersionUID = 8401716646032719628L;

    private final StringBuffer info = new StringBuffer();

    private VersioningActions versioningAction = VersioningActions.ACTION_UNDEFINED;

    private VersioningActions defaultVersioningAction;

    private final List<VersioningActions> options = new ArrayList<VersioningActions>();

    public void addInfo(String info) {
        this.info.append(info);
        this.info.append("; ");
    }

    /**
     * Explanatory information derived from rules logic (could be used in
     * debugging).
     */
    public String getInfo() {
        return info.toString();
    }

    public void setVersioningAction(VersioningActions versioningAction) {
        this.versioningAction = versioningAction;
    }

    /**
     * Gets the versioning action to be proposed to the user.
     * <p>
     * Can be a generic action that will define a list of actions to be
     * presented to the user.
     *
     * @return ACTION_UNDEFINED if not explicitly set. Could be null.
     */
    public VersioningActions getVersioningAction() {
        return versioningAction;
    }

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

    public void clearOptions() {
        options.clear();
        defaultVersioningAction = null;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append(VersionIncEditOptions.class.getSimpleName());
        buf.append('{');
        buf.append("info: ");
        buf.append(info);
        buf.append(", action: ");
        buf.append(versioningAction);
        buf.append(", options: ");
        buf.append(options);
        buf.append('}');

        return buf.toString();
    }

}
