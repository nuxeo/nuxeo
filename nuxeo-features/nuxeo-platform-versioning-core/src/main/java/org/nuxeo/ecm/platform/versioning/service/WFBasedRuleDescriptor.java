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
 * @author dragos
 *
 */
@XObject("versioningRuleWF")
public class WFBasedRuleDescriptor implements RuleDescriptor {

    private static final Log log = LogFactory.getLog(WFBasedRuleDescriptor.class);

    /** The rule name. */
    @XNode("@name")
    private String name;

    @XNode("@workflowStateInitial")
    private String workflowStateInitial;

    @XNode("@workflowStateFinal")
    private String workflowStateFinal;

    @XNode("@action")
    private String action;

    @XNode("@enabled")
    private Boolean enabled = true;

    /**
     * Default constructor - used normally when created as an XObject.
     */
    public WFBasedRuleDescriptor() {
        log.debug("<DocVersioningRuleDescriptor:init>");
    }

    public String getName() {
        return name;
    }

    public String getAction() {
        return action;
    }

    public String getWorkflowStateFinal() {
        return workflowStateFinal;
    }

    public String getWorkflowStateInitial() {
        return workflowStateInitial;
    }

    public boolean isEnabled() {
        return enabled;
    }

}
