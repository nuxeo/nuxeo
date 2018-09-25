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

import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

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

    protected AWSConfigurationDescriptorRegistry registry = new AWSConfigurationDescriptorRegistry();

    protected static class AWSConfigurationDescriptorRegistry extends SimpleContributionRegistry<AWSConfigurationDescriptor> {

        @Override
        public String getContributionId(AWSConfigurationDescriptor contrib) {
            return ""; // unique config
        }

        @Override
        public AWSConfigurationDescriptor clone(AWSConfigurationDescriptor orig) {
            return new AWSConfigurationDescriptor(orig);
        }

        @Override
        public void merge(AWSConfigurationDescriptor src, AWSConfigurationDescriptor dst) {
            dst.merge(src);
        }

        @Override
        public boolean isSupportingMerge() {
            return true;
        }

        public void clear() {
            currentContribs.clear();
        }

        public AWSConfigurationDescriptor getDescriptor() {
            return getCurrentContribution(""); // unique config
        }
    }

    @Override
    public void activate(ComponentContext context) {
        registry.clear();
    }

    @Override
    public void deactivate(ComponentContext context) {
        registry.clear();
    }

    @Override
    public void registerContribution(Object contrib, String xpoint, ComponentInstance contributor) {
        if (XP_CONFIGURATION.equals(xpoint)) {
            addContribution((AWSConfigurationDescriptor) contrib);
        } else {
            throw new RuntimeException("Unknown extension point: " + xpoint);
        }
    }

    @Override
    public void unregisterContribution(Object contrib, String xpoint, ComponentInstance contributor) {
        if (XP_CONFIGURATION.equals(xpoint)) {
            removeContribution((AWSConfigurationDescriptor) contrib);
        } else {
            throw new RuntimeException("Unknown extension point: " + xpoint);
        }
    }

    protected void addContribution(AWSConfigurationDescriptor descriptor) {
        registry.addContribution(descriptor);
    }

    protected void removeContribution(AWSConfigurationDescriptor descriptor) {
        registry.removeContribution(descriptor);
    }

    @Override
    public AWSCredentials getAWSCredentials() {
        AWSConfigurationDescriptor descriptor = registry.getDescriptor();
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
        AWSConfigurationDescriptor descriptor = registry.getDescriptor();
        if (descriptor != null) {
            String region = descriptor.getRegion();
            if (isNotBlank(region)) {
                return region;
            }
        }
        return null;
    }

}
