/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.webapp.helpers;

import java.util.Map;

/**
 * Global resources can be injected by Seam into a application scoped component that doesn't need to be serialized.
 * <p>
 * This circumvents possible Seam bugs in Seam post-activation injection problems regarding resource bundles.
 *
 * @author DM
 * @deprecated since 5.6: this is useless and does not play well with hot reload enabled because component has scope
 *             "Application". Just inject the component named "messages" in components needing translation features,
 *             instead of making them extend this class.
 */
@Deprecated
public interface ResourcesAccessor {

    Map<String, String> getMessages();

}
