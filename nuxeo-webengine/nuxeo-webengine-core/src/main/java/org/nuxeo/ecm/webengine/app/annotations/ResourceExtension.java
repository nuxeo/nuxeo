/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.app.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to annotate extension resources.
 * Extension resources are used to insert new sub-locators to an existing resource.
 * The extension resource will be instantiated and returned when its key match the path segment 
 * on the target resource.
 *   
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ResourceExtension {

    /**
     * The target resource where this resource should be contributed
     * @return
     */
    Class<?> target();
    
    /**
     * The path segment where this resource should be installed. 
     * @return the key
     */
    String key();
    
    /**
     * The contribution categories.
     * Categories can be shared between contributions
     * @return the type 
     */
    String[] categories() default {};
    
}
