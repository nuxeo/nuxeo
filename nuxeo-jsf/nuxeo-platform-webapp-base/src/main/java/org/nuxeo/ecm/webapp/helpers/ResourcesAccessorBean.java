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

package org.nuxeo.ecm.webapp.helpers;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

/**
 * Global resources can be injected by Seam into a application scoped component
 * that doesn't need to be serialized.
 * <p>
 * This circumvents possible Seam bugs in Seam post-activation injection problems
 * regarding resource bundles.
 *
 * @author DM
 */
@Name("resourcesAccessor")
@Scope(ScopeType.APPLICATION)
public class ResourcesAccessorBean implements ResourcesAccessor {

    private static final Log log = LogFactory.getLog(ResourcesAccessorBean.class);

    /**
     * Seam built-in component.
     * <p>
     * A map containing internationalized messages rendered from message
     * templates defined in the Seam resource bundle.
     */
    @In(create = true)
    private Map<String, String> messages;

    public Map<String, String> getMessages() {
        if (messages==null) {
            log.warn("Unable to get message map");
            return new HashMap<String, String>();
        }
        else {
            return messages;
        }
    }

}
