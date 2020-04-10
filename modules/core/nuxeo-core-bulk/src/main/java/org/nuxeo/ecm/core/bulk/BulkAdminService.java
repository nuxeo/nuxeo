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
 *       Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.bulk;

import java.util.List;

/**
 * The Bulk admin service, it's an internal service in order to access configuration from Bulk Action Framework.
 *
 * @since 10.2
 */
// This service is needed by stream processors to create appropriate streams
public interface BulkAdminService {

    /**
     * Returns a list of declared bulk actions. By design a bulk action listen the stream of its own name.
     *
     * @return a list of declared bulk actions
     */
    List<String> getActions();

    int getBucketSize(String action);

    int getBatchSize(String action);

    /**
     * @since 11.1
     */
    String getDefaultScroller(String action);

    /**
     * @since 11.1
     */
    String getInputStream(String action);

    /**
     * Returns true if the action id is to be accessible through http API.
     *
     * @since 10.3
     */
    boolean isHttpEnabled(String actionId);

    /**
     * Returns true if commands about this action need to be executed sequentially instead of being run concurrently.
     *
     * @since 10.3
     */
    boolean isSequentialCommands(String actionId);

    /**
     * Gets the validation class of an action.
     *
     * @since 10.10
     */
    BulkActionValidation getActionValidation(String action);

}
