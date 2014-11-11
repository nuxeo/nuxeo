/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.io.services.enricher;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.ClientException;

/**
 * A Content Enricher knows how to enrich to some Json serialization.
 *
 * @since 5.7.3
 */
public interface ContentEnricher {
    /**
     *
     * @param jg
     * @param ec
     * @throws ClientException
     * @throws IOException
     *
     */
    void enrich(JsonGenerator jg, RestEvaluationContext ec) throws ClientException, IOException;


}
