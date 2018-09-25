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
 *     Remi Cattiau
 *     Florent Guillaume
 */
package org.nuxeo.runtime.aws;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("configuration")
public class AWSConfigurationDescriptor {

    @XNode("accessKeyId")
    protected String accessKeyId;

    @XNode("secretKey")
    protected String secretKey;

    @XNode("sessionToken")
    protected String sessionToken;

    @XNode("region")
    protected String region;

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public String getSessionToken() {
        return sessionToken;
    }

    public String getRegion() {
        return region;
    }

    public AWSConfigurationDescriptor() {
    }

    /** Copy constructor. */
    public AWSConfigurationDescriptor(AWSConfigurationDescriptor other) {
        accessKeyId = other.accessKeyId;
        secretKey = other.secretKey;
        sessionToken = other.sessionToken;
        region = other.region;
    }

    public void merge(AWSConfigurationDescriptor other) {
        if (other.accessKeyId != null) {
            accessKeyId = other.accessKeyId;
        }
        if (other.secretKey != null) {
            secretKey = other.secretKey;
        }
        if (other.sessionToken != null) {
            sessionToken = other.sessionToken;
        }
        if (other.region != null) {
            region = other.region;
        }
    }

}
