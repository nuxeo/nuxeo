/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Laurent Doguin
 */
package org.nuxeo.ecm.core.versioning;

import java.util.Map;

/**
 * Class implementing this interface will be able to use contribution from the
 * versioningRules extension point.
 * 
 * @author Laurent Doguin
 * @since 5.4.1
 */
public interface ExtendableVersioningService extends VersioningService {

    /**
     * @return A Map containing the versioning rule for specific types.
     */
    Map<String, VersioningRuleDescriptor> getVersioningRules();

    /**
     * Add versioning rules for specific types.
     * 
     * @param versioningRules
     */
    void setVersioningRules(
            Map<String, VersioningRuleDescriptor> versioningRules);

    /**
     * Set the default versioning rule for all document type.
     * 
     * @param defaultVersioningRule
     */
    void setDefaultVersioningRule(
            DefaultVersioningRuleDescriptor defaultVersioningRule);

}
