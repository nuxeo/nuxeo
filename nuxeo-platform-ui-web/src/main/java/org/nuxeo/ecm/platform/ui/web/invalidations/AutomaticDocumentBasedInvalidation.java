/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

import javax.interceptor.Interceptors;

/**
 * Annotation for Seam components that will use the automatic Document based
 * invalidation system.
 * <p>
 * On each call, the currentDocument will be passed to a invalidation method
 * (this method must be annotated with "@DocumentContextInvalidation")
 *
 * @author tiry
 */
@Target(TYPE)
@Retention(RUNTIME)
@Interceptors(DocumentContextInvalidatorInterceptor.class)
public @interface AutomaticDocumentBasedInvalidation {

}
