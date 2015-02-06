/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger
 */

package org.nuxeo.ecm.platform.rendition.service;

import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Interface for providers returning a list of {@link RenditionDefinition} for a given {@link DocumentModel}.
 *
 * @since 7.2
 */
public interface RenditionDefinitionProvider {

    List<RenditionDefinition> getRenditionDefinitions(DocumentModel doc);
}
