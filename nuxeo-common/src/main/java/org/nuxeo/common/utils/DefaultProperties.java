/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     jcarsique
 */
package org.nuxeo.common.utils;

import java.util.Properties;

/**
 * At the opposite of {@link Properties} which defaults to its "parent", this class provides default values overridden
 * by its "parent" values which take precedence.
 * <p>
 * {@code new DefaultProperties(System.getProperties())} will return System values at first and fallback on its own
 * values.
 *
 * @since 7.4
 */
public class DefaultProperties extends Properties {

    private static final long serialVersionUID = 1L;

    /**
     * Creates an empty property list with no default values.
     */
    public DefaultProperties() {
        this(null);
    }

    /**
     * Creates an empty property list with the specified defaults.
     *
     * @param defaults the defaults.
     */
    public DefaultProperties(Properties defaults) {
        this.defaults = defaults;
    }

    @Override
    public String getProperty(String key) {
        Object oval = super.get(key);
        String sval = (oval instanceof String) ? (String) oval : null;
        String dval = defaults != null ? defaults.getProperty(key) : null;
        return (dval != null ? dval : sval);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        String val = getProperty(key);
        return (val == null) ? defaultValue : val;
    }

}
