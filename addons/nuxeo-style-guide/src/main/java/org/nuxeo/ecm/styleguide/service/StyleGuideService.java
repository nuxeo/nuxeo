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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.styleguide.service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.faces.context.ExternalContext;

import org.nuxeo.ecm.styleguide.service.descriptors.IconDescriptor;

/**
 * @since 5.7
 */
public interface StyleGuideService extends Serializable {

    /**
     * Returns a map of all icons given a path, creating descriptors from them and putting all unknown icons in the
     * "unknown" category.
     */
    Map<String, List<IconDescriptor>> getIconsByCat(ExternalContext cts, String path);

}
