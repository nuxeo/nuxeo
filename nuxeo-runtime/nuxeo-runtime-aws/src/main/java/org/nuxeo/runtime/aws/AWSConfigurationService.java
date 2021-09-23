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

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;

/**
 * The service providing AWS configuration.
 *
 * @since 10.3
 */
public interface AWSConfigurationService {

    /**
     * Gets the AWS Credentials for the default configuration.
     *
     * @return the AWS credentials, or {@code null} if not defined
     */
    default AWSCredentials getAWSCredentials() {
        return getAWSCredentials(null);
    }

    /**
     * Gets the AWS Credentials for the given configuration.
     *
     * @param id the configuration id, or {@code null} for the default
     * @return the AWS credentials, or {@code null} if not defined
     * @since 11.1
     */
    AWSCredentials getAWSCredentials(String id);

    /**
     * Enriches the given client configuration with an SSL socket factory from the default configuration.
     *
     * @param config the configuration to enrich
     * @since 2021.10
     */
    default void configureSSL(ClientConfiguration config) {
        configureSSL(null, config);
    }

    /**
     * Enriches the given client configuration with an SSL socket factory from the given configuration.
     *
     * @param id the custom configuration id
     * @param config the configuration to enrich
     * @since 2021.10
     */
    void configureSSL(String id, ClientConfiguration config);

    /**
     * Gets the AWS Region for the default configuration.
     *
     * @return the AWS Region, or {@code null} if not defined
     */
    default String getAWSRegion() {
        return getAWSRegion(null);
    }

    /**
     * Gets the AWS Region for the given configuration.
     *
     * @param id the configuration id, or {@code null} for the default
     * @return the AWS Region, or {@code null} if not defined
     * @since 11.1
     */
    String getAWSRegion(String id);

}
