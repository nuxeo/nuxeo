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
 * $Id$
 */

package org.nuxeo.runtime.model;

/**
 * A component extension point.
 * <p>
 * Extension points are described by a name and a list of optional contribution
 * object classes.
 * <p>
 * When defined, the contribution object classes are the type of objects
 * accepted by this extension point.
 * <p>
 * The extension point is also responsible for extracting contribution objects
 * from the extension data, if any.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface ExtensionPoint {

    /**
     * Gets the extension point name.
     *
     * @return the extension point name
     */
    String getName();

    /**
     * Gets the object types of the contributions accepted by this extension point.
     *
     * @return the accepted contribution types
     */
    Class[] getContributions();

    /**
     * Gets the comment attached to this extension point if any.
     *
     * @return the comment
     */
    String getDocumentation();

    /**
     * Get the component owning the base extension which this one extends.
     * <p>
     * If this method returns null, it means the current extension point is
     * extending another extension point and should forward any contribution to
     * the base extension. The base extension has the same name as this one but
     * it is declared in another component.
     *
     * @return the base extension point if this extension point is extending
     *         another extension point, or null if none
     */
    String getSuperComponent();

}
