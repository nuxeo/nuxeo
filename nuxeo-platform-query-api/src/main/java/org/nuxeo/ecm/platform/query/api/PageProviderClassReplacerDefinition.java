/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Benoit Delbosc
 */

package org.nuxeo.ecm.platform.query.api;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Class replacer descriptor interface enable to supersede a class of an
 * existing Page provider.
 *
 * @since 5.9.6
 */
public interface PageProviderClassReplacerDefinition extends Serializable {

    boolean isEnabled();

    void setEnabled(boolean enabled);

    /**
     * Get the map where the key is the class name, the value a list of page
     * provider names.
     */
    Map<String, List<String>> getReplacerMap();

    void setReplacerMap(Map<String, List<String>> map);
}
