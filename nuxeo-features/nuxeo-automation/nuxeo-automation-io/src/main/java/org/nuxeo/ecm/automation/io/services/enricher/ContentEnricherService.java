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
import java.util.List;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.core.api.ClientException;

/**
 * Expose a way to get contributors to rest entities.
 *
 * @since 5.7.3
 */
public interface ContentEnricherService {

    /**
     * Gets contributors for a category
     * <p>
     * Only contributors available in the given context are returned
     */
    List<ContentEnricher> getEnrichers(String category,
            RestEvaluationContext context);

    /**
     * Write some JSon for a given evaluation context.
     *
     * @param jg
     * @param ec
     *
     * @throws IOException
     * @throws JsonGenerationException
     * @throws ClientException
     *
     */
    void writeContext(JsonGenerator jg, RestEvaluationContext ec)
            throws JsonGenerationException, IOException, ClientException;

}
