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
