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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.api.pathsegment;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.runtime.RuntimeServiceException;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Central service for the generation of a path segment for a document.
 */
public class PathSegmentComponent extends DefaultComponent implements PathSegmentService {

    public static final String XP = "pathSegmentService";

    protected static final PathSegmentService DEFAULT_SERVICE = new PathSegmentServiceDefault();

    protected PathSegmentService service;

    @Override
    public void start(ComponentContext context) {
        service = this.<PathSegmentServiceDescriptor> getRegistryContribution(XP).map(desc -> {
            try {
                return (PathSegmentService) desc.className.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeServiceException(e);
            }
        }).orElse(DEFAULT_SERVICE);
    }

    @Override
    public void stop(ComponentContext context) throws InterruptedException {
        service = null;
    }

    @Override
    public String generatePathSegment(DocumentModel doc) {
        return service.generatePathSegment(doc);
    }

    @Override
    public String generatePathSegment(String s) {
        return service.generatePathSegment(s);
    }

    @Override
    public int getMaxSize() {
        return service.getMaxSize();
    }

}
