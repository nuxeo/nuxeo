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

import com.amazonaws.auth.AWSCredentials;

/**
 * The service providing AWS configuration.
 *
 * @since 10.3
 */
public interface AWSConfigurationService {

    /**
     * Gets the AWS Credentials.
     *
     * @return the AWS credentials, or {@code null} if not defined
     */
    AWSCredentials getAWSCredentials();

    /**
     * Gets the AWS Region.
     *
     * @return the AWS Region, or {@code null} if not defined
     */
    String getAWSRegion();

}
