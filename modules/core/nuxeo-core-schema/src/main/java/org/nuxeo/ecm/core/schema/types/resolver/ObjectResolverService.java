/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.schema.types.resolver;

import java.util.Map;

/**
 * Provides a way to instanciate {@link ObjectResolver}.
 *
 * @since 7.1
 */
public interface ObjectResolverService {

    /**
     * @param type the xsd type, a resolver could manage.
     * @param parameters the parameters for this resolver.
     * @since 7.1
     */
    ObjectResolver getResolver(String type, Map<String, String> parameters);

}
