/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.gwt.client;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Bundle {
    
    /**
     * A list of bundles to extend. The order is important.
     * Bundles will be processed in the order they appear in this array after processing first the current bundle.
     * This means for a bundle declaration:
     * <pre>
     * @Bundle(B1.class, B2.class)
     * public interface B0 {
     * ...
     * </pre>
     * the processing order is B0, B1, B2.
     * 
     * The default value is an empty list when no bundles are extended.
     * @return
     */
    Class<?>[] value() default {};
    
    /**
     * The XML descriptor file for the extensions 
     * @return
     */
    String descriptor() default "";
    
}
