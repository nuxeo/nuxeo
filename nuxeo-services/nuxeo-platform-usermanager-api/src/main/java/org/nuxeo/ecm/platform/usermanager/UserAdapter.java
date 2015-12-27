/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     tmartins
 */
package org.nuxeo.ecm.platform.usermanager;

import java.util.List;


/**
 * Interface to expose user model fields
 *
 * @since 5.7
 * @author <a href="mailto:tm@nuxeo.com">Thierry Martins</a>
 */
public interface UserAdapter {

    String getName();

    String getFirstName();

    String getLastName();

    String getEmail();

    String getCompany();

    List<String> getGroups();

    /**
     * @since 8.1
     */
    String getTenantId();

    String getSchemaName();
}
