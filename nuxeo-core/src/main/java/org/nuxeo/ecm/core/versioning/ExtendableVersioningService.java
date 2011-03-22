/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
 * @since 5.4.2
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
