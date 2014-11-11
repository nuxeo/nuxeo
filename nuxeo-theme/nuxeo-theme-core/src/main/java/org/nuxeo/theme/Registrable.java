/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme;

/**
 * Objects registered in the ThemeService must implement this interface.
 * 
 * @author Jean-Marc Orliaguet
 */
public interface Registrable {

    /**
     * This method is called when the object is unregistered. Clean up local
     * variables here.
     */
    void clear();

}
