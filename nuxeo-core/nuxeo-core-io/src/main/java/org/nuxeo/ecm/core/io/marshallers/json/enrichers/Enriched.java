/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nicolas Chapurlat <nchapurlat@nuxeo.com>
 */

package org.nuxeo.ecm.core.io.marshallers.json.enrichers;

import org.nuxeo.ecm.core.io.marshallers.json.ExtensibleEntityJsonWriter;

/**
 * Technical wrapper that embed an enriched entity for a broadcast to related enrichers. see
 * {@link ExtensibleEntityJsonWriter} and {@link AbstractJsonEnricher} to understand how it works.
 *
 * @param <EntityType> The Java type to enrich.
 * @since 7.2
 */
public class Enriched<EntityType> {

    private EntityType enrichable;

    public Enriched(EntityType enrichable) {
        super();
        this.enrichable = enrichable;
    }

    public EntityType getEntity() {
        return enrichable;
    }

}
