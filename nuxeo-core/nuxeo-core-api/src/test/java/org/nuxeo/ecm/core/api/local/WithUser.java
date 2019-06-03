/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  Contributors:
 *      Kevin Leturc <kleturc@nuxeo.com>
 */
package org.nuxeo.ecm.core.api.local;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.nuxeo.ecm.core.api.security.SecurityConstants;

/**
 * Interface allowing to login as a specific user into Nuxeo Platform.
 * <p>
 * It leverages {@link DummyLoginAs} class and so log in as {@code Administrator} or {@code anonymous} will give
 * specific roles.
 *
 * @since 11.1
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface WithUser {

    /**
     * @return the username to use to login
     */
    @SuppressWarnings("deprecation")
    String value() default SecurityConstants.ADMINISTRATOR;
}
