/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id: ScopeType.java 19046 2007-05-21 13:03:50Z sfermigier $
 */

package org.nuxeo.common.collections;

/**
 * Scope type definitions for a scoped map.
 * <p>
 * Only request and default scopes are defined for now, but others may be added.
 *
 * @see ScopedMap
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public enum ScopeType {
    DEFAULT, REQUEST;

    public String getScopedKey(String key) {
        return getScopePrefix() + key;
    }

    public String getScopePrefix() {
        return name().toLowerCase() + '/';
    }

}
