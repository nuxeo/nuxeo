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

package org.nuxeo.ecm.core.io.registry.reflect;

import org.nuxeo.ecm.core.io.registry.context.RenderingContext;

/**
 * Define the instantiation mode for this a marshaller.
 * <p>
 * The use of {@link #SINGLETON} is highly recommended.
 * </p>
 * <p>
 * {@link #SINGLETON} marshallers are more priority than others.
 * </p>
 *
 * @since 7.2
 */
public enum Instantiations {

    /**
     * The marshaller is instantiated once.
     * <p>
     * Each class with this instantiation mode should just have thread safe properties, or injected properties.
     * </p>
     * <p>
     * Please note that injected {@link RenderingContext} is thread safe.
     * </p>
     */
    SINGLETON,

    /**
     * One instance of the marshaller is created per thread.
     */
    PER_THREAD,

    /**
     * One instance of marshaller is created for each marshalling request.
     */
    EACH_TIME;

}
