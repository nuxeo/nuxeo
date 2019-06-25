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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to define a console log level threshold on the root logger, which allows us to hide console log messages
 * when launching tests. Any {@code Class} or {@code Method} can be annotated, when a {@code Method} is marked then the
 * console level will override the {@code Class} level if it is defined and it will be restored after the end of the
 * execution of this {@code Method} otherwise it will inherit the {@code Class} defined level.
 * <p>
 * The default level value is {@link java.util.logging.Level#OFF} which means all console log messages will be hidden.
 *
 * @since 11.1
 */
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface ConsoleLogLevelThreshold {

    String value() default "OFF";
}
