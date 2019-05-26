/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.invalidations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.jboss.seam.annotations.intercept.Interceptors;

/**
 * Annotation for Seam components that will use the automatic Document based invalidation system.
 * <p>
 * On each call, the currentDocument will be passed to a invalidation method (this method must be annotated with
 * "@DocumentContextInvalidation")
 *
 * @author tiry
 */
@Target(TYPE)
@Retention(RUNTIME)
@Interceptors(DocumentContextInvalidatorInterceptor.class)
public @interface AutomaticDocumentBasedInvalidation {

}
