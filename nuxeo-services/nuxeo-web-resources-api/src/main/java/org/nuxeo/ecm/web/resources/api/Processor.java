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
package org.nuxeo.ecm.web.resources.api;

import java.util.List;

/**
 * Resource processor.
 *
 * @since 7.3
 */
public interface Processor extends Comparable<Processor> {

    /**
     * Processor name, to be registered as an alias on wro.
     */
    String getName();

    /**
     * Boolean flag controlling enablement of a processor.
     */
    boolean isEnabled();

    /**
     * Flag type markers for processors filtering depending on use cases.
     */
    List<String> getTypes();

    int getOrder();

    /**
     * Returns the target processor class.
     * <p>
     * Does not follow any given interface to avoid adherence to a given processing implementation.
     */
    Class<?> getTargetProcessorClass();

}
