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

import com.google.gwt.user.client.ui.Widget;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface View<W extends Widget> {

    /**
     * Get the view name the view an can be used to find out a view from its container
     * @return
     */
    public String getName();

    /**
     * Get the view icon path if any.
     * @return the icon path or null if none.
     */
    public String getIcon();

    /**
     * Set a icon for this view
     * @param icon the icon path
     */
    public void setIcon(String icon);
    
    /**
     * Get the view title. The title will be show in the UI by the container
     * if the container support this.
     * If null the view name will be used 
     * @return the title. can be null.
     */
    public String getTitle();

    
    /**
     * Set a title for this view
     * @param title
     */
    public void setTitle(String title);

    /**
     * Called by the container (if container supports refresh)
     * when application context change
     */
    public void refresh();

    /**
     * Show busy state. Called when busy state is required.
     */
    public void showBusy();

    /**
     * Hide busy state. Called when busy state terminated.
     */
    public void hideBusy();

    /**
     * Whether or not this view accepts the given input.
     * This is an optional operation and should return false if not supported  
     * @param input
     * @return true if input can be rendered by the view
     */
    public boolean acceptInput(Object input);
    
    /**
     * Set the input object for the view.
     * This is an optional operation and should be implemented only by views
     * that supports input (e.g. editors)
     * @param input
     */
    public void setInput(Object input);

    /**
     * Get the current input if the view if any
     * @return the current input or null if none
     */
    public Object getInput();
    
    /**
     * Get the widget bound to this view.
     * If no one is already bound a new one should be created 
     * @return
     */
    public W getWidget();

    /**
     * Check whether this view is attached to an widget
     * @return true if attached, false otherwise
     */
    public boolean isAttached();
    
    /**
     * Whether the widget attached to this view is visible.
     * If not yet attached then return false;
     * @return
     */
    public boolean isVisible();
    
    /**
     * Destroy the widget bound to this view. 
     * A destroyed widget cannot be used anymore and will be eligible for garbage collection..
     * If you want to reuse later the widget then just disconect it using detach and then reconnect using an attach 
     * The next time {@link #getWidget()} is called a new widget should be created. 
     */
    public void destroy();
    
    /**
     * This method should be called to remove the view widget from its parent.
     * This is not destroying the widget. 
     * You can reuse it later by putting it in the same or another container.  
     */
    public void detachWidget();
    
}
