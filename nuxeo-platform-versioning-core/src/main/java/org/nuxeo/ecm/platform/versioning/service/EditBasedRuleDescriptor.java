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

/**
 * Version incrementation rule descriptor that is considered when a document is
 * about to be edited or when saved (directly by the user). When a document is
 * about to be edited, a rule of this type can give the incrementation options
 * to be displayed in the page.
 * <p>
 * To decide whether or not a rule of this type is a candidate, the service
 * checks for the following document properties:
 * <ul>
 * <li>lifecycleState : the document has to be in the specified state. The
 * wildcard '*' can be used to specify any lifecycle state.</li>
 * <li>document type
 * <ul>
 * <li> if it is specifically included: then the rule applies only for that
 * document type is </li>
 * <li> if it is specifically excluded: then the rule applies to all the other
 * doc types </li>
 * </ul>
 * </li>
 * </ul>
 *
 * @author DM
 *
 */
@XObject("versioningRuleEdit")
public class EditBasedRuleDescriptor implements RuleDescriptor {

    private static final Log log = LogFactory.getLog(EditBasedRuleDescriptor.class);

    /** The rule name. */
    @XNode("@name")
    private String name;

    @XNode("@action")
    private String action;

    @XNode("@lifecycleState")
    private String lifecycleState;

    @XNodeList(value = "option", type = RuleOptionDescriptor[].class, componentType = RuleOptionDescriptor.class)
    private RuleOptionDescriptor[] options;

    @XNodeList(value = "includeDocType", type = String[].class, componentType = String.class)
    private String[] includeDocTypes;

    @XNodeList(value = "excludeDocType", type = String[].class, componentType = String.class)
    private String[] excludeDocTypes;

    @XNode("@enabled")
    private boolean enabled = true;

    /**
     * Default constructor - used normally when created as an XObject.
     */
    public EditBasedRuleDescriptor() {
        log.debug("<EditBasedRuleDescriptor:init>");
    }

    public String getName() {
        return name;
    }

    public String getAction() {
        return action;
    }

    public String getLifecycleState() {
        return lifecycleState;
    }

    public RuleOptionDescriptor[] getOptions() {
        return options;
    }

    public String[] getExcludeDocTypes() {
        return excludeDocTypes;
    }

    public boolean isDocTypeExcluded(String docType) {
        for (String excludedDocType : excludeDocTypes) {
            if (excludedDocType.equals(docType)) {
                return true;
            }
        }
        return false;
    }

    public boolean isDocTypeIncluded(String docType) {
        if (includeDocTypes.length == 0) {
            return true;
        }
        for (String includedDocType : includeDocTypes) {
            if (includedDocType.equals(docType)) {
                return true;
            }
        }
        return false;
    }

    public boolean isDocTypeAccounted(String docType) {
        return !isDocTypeExcluded(docType) && isDocTypeIncluded(docType);
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" {name=").append(name).append('}');
        return sb.toString();
    }

}
