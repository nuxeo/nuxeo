/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

