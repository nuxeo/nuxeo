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

import static org.apache.commons.lang3.StringUtils.defaultString;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.Descriptor;

@XObject("configuration")
public class AWSConfigurationDescriptor implements Descriptor {

    @XNode("accessKeyId")
    protected String accessKeyId;

    @XNode("secretKey")
    protected String secretKey;

    @XNode("sessionToken")
    protected String sessionToken;

    @XNode("region")
    protected String region;

    @Override
    public String getId() {
        return UNIQUE_DESCRIPTOR_ID;
    }

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

    @Override
    public AWSConfigurationDescriptor merge(Descriptor o) {
        AWSConfigurationDescriptor other = (AWSConfigurationDescriptor) o;
        AWSConfigurationDescriptor merged = new AWSConfigurationDescriptor();
        merged.accessKeyId = defaultString(other.accessKeyId, accessKeyId);
        merged.secretKey = defaultString(other.secretKey, secretKey);
        merged.sessionToken = defaultString(other.sessionToken, sessionToken);
        merged.region = defaultString(other.region, region);
        return merged;
    }

}
