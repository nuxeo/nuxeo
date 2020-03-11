/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * $Id: PermissionProviderLocal.java 28325 2007-12-24 08:29:26Z sfermigier $
 */

package org.nuxeo.ecm.core.security;

import org.nuxeo.ecm.core.api.security.PermissionProvider;

public interface PermissionProviderLocal extends PermissionProvider {

    void registerDescriptor(PermissionDescriptor descriptor);

    void unregisterDescriptor(PermissionDescriptor descriptor);

    void registerDescriptor(PermissionVisibilityDescriptor descriptor);

    void unregisterDescriptor(PermissionVisibilityDescriptor descriptor);

}
