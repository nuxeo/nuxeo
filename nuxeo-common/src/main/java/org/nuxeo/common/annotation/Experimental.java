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
 *     jcarsique
 */
package org.nuxeo.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Signifies that annotated API is still in early stage and subject to incompatible changes, or even removal, in a
 * future release. The presence of this annotation implies nothing about the code quality or performance, only the fact
 * that the API is not yet "frozen".
 * <p>
 * It is generally safe to depend on Experimental APIs, at the prospective cost of some extra work during upgrades. The
 * API <code>@Since</code> annotation gives a preview on how much mature it is and the chances for the
 * <code>@Experimental</code> annotation being soon removed.
 *
 * @since 7.2
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.PACKAGE, ElementType.CONSTRUCTOR, ElementType.FIELD, ElementType.METHOD, ElementType.TYPE })
@Documented
@Experimental
public @interface Experimental {
    String comment() default "";
}
