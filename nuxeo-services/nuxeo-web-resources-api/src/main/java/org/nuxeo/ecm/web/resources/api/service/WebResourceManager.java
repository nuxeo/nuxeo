/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.web.resources.api.service;

import java.util.List;

import org.nuxeo.ecm.web.resources.api.Processor;
import org.nuxeo.ecm.web.resources.api.Resource;
import org.nuxeo.ecm.web.resources.api.ResourceBundle;
import org.nuxeo.ecm.web.resources.api.ResourceContext;
import org.nuxeo.runtime.service.TimestampedService;

/**
 * Service for web resources retrieval.
 *
 * @since 7.3
 */
public interface WebResourceManager extends TimestampedService {

    /**
     * Returns a registered resource with given name, or null if not found.
     * <p>
     * Referenced resource can either be a static resource or a style.
     */
    Resource getResource(String name);

    /**
     * Returns a registered resource bundle with given name, or null if not found.
     */
    ResourceBundle getResourceBundle(String name);

    /**
     * Returns all resource bundles registered on the service.
     */
    List<ResourceBundle> getResourceBundles();

    /**
     * Returns the corresponding processor with given name, or null if not found.
     */
    Processor getProcessor(String name);

    /**
     * Returns all processors registered on the service, ordered.
     */
    List<Processor> getProcessors();

    /**
     * Returns all processors registered on the service, ordered, for given type.
     */
    List<Processor> getProcessors(String type);

    /**
     * Returns the ordered list of resources for given bundle name, filtered using given type.
     * <p>
     */
    List<Resource> getResources(ResourceContext context, String bundleName, String type);

    /**
     * Allows to dynamically register a bundle.
     *
     * @since 7.4
     */
    void registerResourceBundle(ResourceBundle bundle);

    /**
     * Allows to dynamically unregister a bundle.
     *
     * @since 7.4
     */
    void unregisterResourceBundle(ResourceBundle bundle);

    /**
     * Allows to dynamically register a resource.
     *
     * @since 7.4
     */
    void registerResource(Resource resource);

    /**
     * Allows to dynamically unregister a resource.
     *
     * @since 7.4
     */
    void unregisterResource(Resource resource);

}
