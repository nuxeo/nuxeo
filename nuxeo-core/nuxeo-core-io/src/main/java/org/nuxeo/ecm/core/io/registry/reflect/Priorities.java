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

/**
 * Some priorities constants used for annotation's property {@link Setup#priority()}.
 * <p>
 * {@link #REFERENCE} is the default priority for Nuxeo marshallers. If you want to override a Nuxeo marshaller, be very
 * carreful about consequences and use an higher priority like {@link #OVERRIDE_REFERENCE}.
 * </p>
 *
 * @since 7.2
 */
public interface Priorities {

    int DEFAULT = 0;

    int DERIVATIVE = 1000;

    /**
     * Most of Nuxeo builtin marshallers.
     */
    int REFERENCE = 2000;

    int OVERRIDE_REFERENCE = 3000;

}
