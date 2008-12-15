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

package org.nuxeo.ecm.platform.gwt.client.ui;




/**
 * An view that support input objects. Usually used by views to implement editors.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface Editor {

    /**
     * Whether or not this view accepts the given input
     * @param input
     * @return true if input can be rendered by the view
     */
    public boolean acceptInput(Object input);
    
    /**
     * Open the given input. Return a view that render the editor
     * @param input
     * @return
     */
    public View<?> open(Object input);
    
    /**
     * Tests whether this editor can reuse views.
     * If not each time an editor is asked for a view a new view will be created.
     * For example into a tabbed editor container a new tab will be created each time an editor open an input object.
     * If you want to reuse existing views (tabs) created by the same editor you should return true from that method.
     * @return true if can reuse views, false otherwise.
     */
    public boolean canReuseViews();
    
}
