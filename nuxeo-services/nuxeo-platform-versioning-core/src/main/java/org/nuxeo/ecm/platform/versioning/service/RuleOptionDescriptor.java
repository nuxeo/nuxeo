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
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Rule option descriptor.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
@XObject("versioningRuleEdit")
public class RuleOptionDescriptor {

    private static final Log log = LogFactory.getLog(RuleOptionDescriptor.class);

    /** The option value. */
    @XNode("@value")
    private String value;

    /**
     *  Lifecycle transition that will be performed if the option is selected.
     *  May be null.
     */
    @XNode("@lifecycleTransition")
    private String lifecycleTransition;

    /**
     * This field is used to tell if the value is the default one.
     */
    @XNode("@default")
    private boolean isDefault;

    /**
     * Default constructor - used normally when created as an XObject.
     */
    public RuleOptionDescriptor() {
        log.debug("<AutoBasedRuleDescriptor:init>");
    }

    public String getValue() {
        return value;
    }

    public String getLifecycleTransition() {
        return lifecycleTransition;
    }

    public boolean isDefault() {
        return isDefault;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(getClass().getSimpleName());
        buf.append(" {value=").append(value);
        buf.append(", isDefault=").append(isDefault);
        buf.append(", lifecycleTransition=").append(lifecycleTransition);
        buf.append('}');

        return buf.toString();
    }

}
