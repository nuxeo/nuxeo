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

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.Descriptor;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.BasicSessionCredentials;

/**
 * Implementation of the service providing AWS configuration.
 *
 * @since 10.3
 */
public class AWSConfigurationServiceImpl extends DefaultComponent implements AWSConfigurationService {

    public static final String XP_CONFIGURATION = "configuration";

    protected AWSConfigurationDescriptor getDescriptor() {
        return getDescriptor(XP_CONFIGURATION, Descriptor.UNIQUE_DESCRIPTOR_ID);
    }

    @Override
    protected String getName() {
        return "org.nuxeo.runtime.aws.AWSConfigurationService";
    }

    @Override
    public AWSCredentials getAWSCredentials() {
        AWSConfigurationDescriptor descriptor = getDescriptor();
        if (descriptor != null) {
            String accessKeyId = descriptor.getAccessKeyId();
            String secretKey = descriptor.getSecretKey();
            String sessionToken = descriptor.getSessionToken();
            if (isNotBlank(accessKeyId) && isNotBlank(secretKey)) {
                if (isNotBlank(sessionToken)) {
                    return new BasicSessionCredentials(accessKeyId, secretKey, sessionToken);
                } else {
                    return new BasicAWSCredentials(accessKeyId, secretKey);
                }
            }
        }
        return null;
    }

    @Override
    public String getAWSRegion() {
        AWSConfigurationDescriptor descriptor = getDescriptor();
        if (descriptor != null) {
            String region = descriptor.getRegion();
            if (isNotBlank(region)) {
                return region;
            }
        }
        return null;
    }

}
