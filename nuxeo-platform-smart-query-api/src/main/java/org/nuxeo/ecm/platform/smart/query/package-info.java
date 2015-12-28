/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

/**
 * Provides the interface for a {@link SmartQuery}.
 * <p>
 * Also defines the abstract class {@link IncrementalSmartQuery} that
 * implements this interface: it provides getters and setters needed for JSF
 * interactions when constructing a query part by part, e.g adding criterion to
 * an existing query string.
 * <p>
 * The class {@link HistoryList} is useful for undo operations.
 *
 * @since 5.4
 */
package org.nuxeo.ecm.platform.smart.query;

