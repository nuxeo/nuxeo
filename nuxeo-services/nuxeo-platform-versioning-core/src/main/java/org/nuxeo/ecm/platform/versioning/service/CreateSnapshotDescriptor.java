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

package org.nuxeo.ecm.platform.versioning.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.versioning.api.SnapshotOptions;

/**
 * Defines rule for displaying option or automatically create a document
 * snapshot before updating.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
@XObject("createSnapshot")
public class CreateSnapshotDescriptor {

    private static final Log log = LogFactory.getLog(CreateSnapshotDescriptor.class);

    /** The rule name. */
    @XNode("@name")
    private String name;

    @XNode("@optional")
    private boolean optional;

    /**
     * Specify if the createSnapshot is default selected (in case it is displayed)
     * or if the snapshot will be automatically created (in case the option is not displayed).
     */
    @XNode("@default")
    private boolean defaultCreate;

    @XNodeList(value = "lifecycleState", type = String[].class, componentType = String.class)
    private String[] lifecycleStates;

    /**
     * Default constructor - used normally when created as an XObject.
     */
    public CreateSnapshotDescriptor() {
        log.debug("<CreateSnapshotDescriptor:init>");
    }

    public String getName() {
        return name;
    }

    public boolean getOptional() {
        return optional;
    }

    public boolean getDefaultCreate() {
        return defaultCreate;
    }

    public String[] getLifecycleStates() {
        return lifecycleStates;
    }

    public boolean applyForLifecycleState(String lifecycleState) {
        if (lifecycleState == null) {
            throw new IllegalArgumentException("null lifecycleState");
        }
        for (String lcState : lifecycleStates) {
            if (lcState.equals("*") || lcState.equals(lifecycleState)) {
                return true;
            }
        }
        return false;
    }

    public SnapshotOptions getSnapshotOption() {
        if (optional) {
            if (defaultCreate) {
                return SnapshotOptions.DISPLAY_SELECTED;
            } else {
                return SnapshotOptions.DISPLAY_NOT_SELECTED;
            }
        } else {
            return SnapshotOptions.NOT_DISPLAYED;
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" {name=").append(name).append('}');
        return sb.toString();
    }

}
