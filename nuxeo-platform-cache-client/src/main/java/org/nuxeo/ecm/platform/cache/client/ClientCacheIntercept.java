/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: LogEntryCallbackListener.java 16046 2007-04-12 14:34:58Z fguillaume $
 */

package org.nuxeo.ecm.platform.cache.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TODO : not used at the moment because it blows up.
 *
 * The annotation to mark the EJB interface methods that will be intercepted and
 * whom result is going to be searched in the cache for
 *
 * @author DM
 *
 */
@Inherited
@Retention(value = RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface ClientCacheIntercept {

    // TODO have to define needed attributes
    //public int docRefParamPos() default {};
    //public Enum returnType() default {Enum.DOCUMENT_MODEL...};
}
