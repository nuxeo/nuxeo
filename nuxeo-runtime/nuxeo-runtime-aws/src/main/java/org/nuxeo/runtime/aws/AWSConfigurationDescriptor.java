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

    public static final String DEFAULT_CONFIG_ID = "default";

    @XNode("@id")
    protected String id = DEFAULT_CONFIG_ID;

    @XNode("accessKeyId")
    protected String accessKeyId;

    @XNode("secretKey")
    protected String secretKey;

    @XNode("sessionToken")
    protected String sessionToken;

    @XNode("region")
    protected String region;

    /** @since 2021.10 */
    @XNode("trustStorePath")
    protected String trustStorePath;

    /** @since 2021.10 */
    @XNode("trustStorePassword")
    protected String trustStorePassword;

    /** @since 2021.10 */
    @XNode("trustStoreType")
    protected String trustStoreType;

    /** @since 2021.10 */
    @XNode("keyStorePath")
    protected String keyStorePath;

    /** @since 2021.10 */
    @XNode("keyStorePassword")
    protected String keyStorePassword;

    /** @since 2021.10 */
    @XNode("keyStoreType")
    protected String keyStoreType;

    @Override
    public String getId() {
        return id;
    }

    /** @since 2021.10 */
    public String getTrustStorePath() {
        return trustStorePath;
    }

    /** @since 2021.10 */
    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    /** @since 2021.10 */
    public String getTrustStoreType() {
        return trustStoreType;
    }

    /** @since 2021.10 */
    public String getKeyStorePath() {
        return keyStorePath;
    }

    /** @since 2021.10 */
    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    /** @since 2021.10 */
    public String getKeyStoreType() {
        return keyStoreType;
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
        merged.trustStorePath = defaultString(other.trustStorePath, trustStorePath);
        merged.trustStorePassword = defaultString(other.trustStorePassword, trustStorePassword);
        merged.trustStoreType = defaultString(other.trustStoreType, trustStoreType);
        merged.keyStorePath = defaultString(other.keyStorePath, keyStorePath);
        merged.keyStorePassword = defaultString(other.keyStorePassword, keyStorePassword);
        merged.keyStoreType = defaultString(other.keyStoreType, keyStoreType);
        return merged;
    }

}
