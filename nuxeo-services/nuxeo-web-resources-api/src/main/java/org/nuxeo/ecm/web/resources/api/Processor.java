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
