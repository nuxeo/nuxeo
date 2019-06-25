/*
 * (C) Copyright 2019 Nuxeo (http://nuxeo.com/) and others.
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
 *     Salem Aouana
 */

package org.nuxeo.runtime.test.runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that will help you managing the logging when dealing with tests. You can create or update a logger
 * dynamically by annotating your class or method test.
 *
 * @since 11.1
 */
@Inherited
@Repeatable(LoggerLevels.class)
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface LoggerLevel {

    /** The logger name on which we want to set the logger level **/
    String name() default "";

    /** The logger level to set **/
    String level();

    /** The logger class on which we want to set the logger level **/
    Class<?> klass() default Object.class;
}
