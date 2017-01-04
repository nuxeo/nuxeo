/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.snapshot;

import org.nuxeo.ecm.core.api.VersioningOption;

public interface Snapshotable {

    public static final String FACET = "Snapshotable";

    public static final String ABOUT_TO_CREATE_LEAF_VERSION_EVENT = "aboutToCreateLeafVersionEvent";

    public static final String ROOT_DOCUMENT_PROPERTY = "leafRootDocument";

    Snapshot createSnapshot(VersioningOption option);

}
