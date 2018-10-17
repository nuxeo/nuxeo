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
package org.nuxeo.runtime.aws;

import static org.apache.commons.lang3.StringUtils.defaultString;

import org.nuxeo.runtime.api.Framework;

import com.amazonaws.SdkClientException;
import com.amazonaws.regions.AwsRegionProvider;
import com.amazonaws.regions.DefaultAwsRegionProviderChain;

/**
 * AWS Region Provider that uses Nuxeo configuration, or uses the default AWS chain as a fallback.
 *
 * @since 10.3
 */
public class NuxeoAWSRegionProvider extends AwsRegionProvider {

    protected static final AwsRegionProvider INSTANCE = new NuxeoAWSRegionProvider();

    protected static final AwsRegionProvider DEFAULT = new DefaultAwsRegionProviderChain();

    protected static final String DEFAULT_REGION = "us-east-1";

    public static AwsRegionProvider getInstance() {
        return INSTANCE;
    }

    @Override
    public String getRegion() {
        AWSConfigurationService service = Framework.getService(AWSConfigurationService.class);
        if (service != null) {
            String region = service.getAWSRegion();
            if (region != null) {
                return region;
            }
        }
        String region;
        try {
            region = DEFAULT.getRegion();
        } catch (SdkClientException e) {
            // the DefaultAwsRegionProviderChain throws when there's no provider instead of defaulting to null
            region = null;
        }
        return defaultString(region, DEFAULT_REGION);
    }

}
