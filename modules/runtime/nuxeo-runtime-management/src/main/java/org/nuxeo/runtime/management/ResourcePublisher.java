/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Stephane Lacoin (Nuxeo EP Software Engineer)
 */
package org.nuxeo.runtime.management;

import java.util.Set;

import javax.management.ObjectName;

/**
 * @author Stephane Lacoin (Nuxeo EP Software Engineer)
 */
public interface ResourcePublisher {

    Set<String> getShortcutsName();

    Set<ObjectName> getResourcesName();

    ObjectName lookupName(String name);

    void registerResource(String shortName, String qualifiedName, Class<?> managementClass, Object instance);

    void unregisterResource(String shortName, String qualifiedName);
}
