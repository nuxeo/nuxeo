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
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Extension {

    public final static int DEFAULT = -1;
    public final static int APPEND = -2;
    public final static int REPLACE = -3;
    public final static int AS_DEFAULT = -4;
    
    /**
     * A list of target extension points
     * @return
     */
    String[] targets();
    
    /**
     * The way extension is contributed. This is a hint to the extension point.
     * Some extension points may simply ignore this.
     * <p>
     * Remember that hints are dependent on extension point capabilities. So before using them you must 
     * read the target extension point documentation.
     * Also, implementors MUST use a coherent hint system. 
     * It is recommended to follow the system described below. 
     *<p>
     * There are two type of extension points:
     * <ol>
     * <li> Containers - that manage a set of contributions.
     * This type of containers may be ordered so having an index to control the order 
     * when inserting contributions may be important. 
     * So in this case the hint can be used to either specify an index >=0, either specify the APPEND value 
     * (which usually have the same meaning as DEFAULT)
     * 
     * <li> Providers - that manage only one contribution
     * This type of containers have no ordering but may wants to know if the contribution must replace the existing one
     * or if it should be merged against the existing one.
     * For this type of extension points we provide 3 hints:
     * <ul>
     * <li>APPEND - merge with existing contribution if any 
     * <li>REPLACE - replace existing contribution if any
     * <li> AS_DEFAULT - deploy it only if no previous contribution was deployed
     * </ul>
     * </ol>
     * 
     * Note that we define a DEFAULT value that can be used to specify the DEFAULT registration hint defined
     * by the target container. The DEFAULT is usually an APPEND or a REPLACE.   
     * 
     * Also, note that when using indexes >= 0 to control the order the indexes may have any value. They are not representing 
     * the real index in the target container but a sort of 'weight' to be used to control ordering.
     * 
     * @return
     */
    int hint() default DEFAULT;
    
}
