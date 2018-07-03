/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.jwt;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for the {@link JWTService}.
 *
 * @since 10.3
 */
@XObject(value = "configuration")
public class JWTServiceConfigurationDescriptor {

    public static final int DEFAULT_MAX_TTL = 60 * 60; // 1h

    @XNode("secret")
    public String secret;

    @XNode("maxTTL")
    public Integer maxTTL;

    public String getSecret() {
        return secret;
    }

    public int getMaxTTL() {
        return maxTTL == null ? DEFAULT_MAX_TTL : maxTTL.intValue();
    }

    /** Empty constructor, to get defaults. */
    public JWTServiceConfigurationDescriptor() {
    }

    /** Copy constructor. */
    public JWTServiceConfigurationDescriptor(JWTServiceConfigurationDescriptor other) {
        this.secret = other.secret;
        this.maxTTL = other.maxTTL;
    }

    /** Merge method. */
    public void merge(JWTServiceConfigurationDescriptor other) {
        if (other.secret != null) {
            secret = other.secret;
        }
        if (other.maxTTL != null) {
            maxTTL = other.maxTTL;
        }
    }

}
