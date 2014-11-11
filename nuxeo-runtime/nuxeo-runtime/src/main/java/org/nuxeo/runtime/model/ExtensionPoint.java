/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
