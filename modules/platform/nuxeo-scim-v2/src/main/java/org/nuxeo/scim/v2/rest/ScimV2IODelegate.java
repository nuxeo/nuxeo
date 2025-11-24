/*
 * (C) Copyright 2025 Nuxeo (http://nuxeo.com/) and others.
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
 *     Kevin Leturc <kevin.leturc@hyland.com>
 */
package org.nuxeo.scim.v2.rest;

import static com.unboundid.scim2.common.utils.ApiConstants.MEDIA_TYPE_SCIM;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;

import javax.inject.Singleton;
import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;

import org.nuxeo.ecm.webengine.jaxrs.coreiodelegate.CoreIODelegate;

/**
 * @since 2025.12
 */
@Singleton
@Provider
@Produces({ MEDIA_TYPE_SCIM, APPLICATION_JSON })
public class ScimV2IODelegate extends CoreIODelegate {
}
