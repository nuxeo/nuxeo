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
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface View extends Drawable {
    
    /**
     * Install the view into a site and sets is input to the given one.
     * The view widget will be created at this point after setting the input.
     * @param site the site to host the view
     * @param input the input object
     */
    public void install(Site site, Object input);
    
    /**
     * Uninstall the view from its site. 
     * All cached data should be removed such as the view widget etc.
     * The view itself should not be destroyed to be able to re-install it later if needed. 
     */
    public void uninstall();
    
    /**
     * Whether or not this view is installed into a site (its widget was created).
     * @return true if installed, false otherwise.
     */
    public boolean isInstalled();
   
    /**
     * Whether or not the view widget was created.
     * @return
     */
   boolean hasWidget();
   
    /**
     * Get the view name the view an can be used to find out a view from its container
     * @return
     */
    public String getName();

    /**
     * Get a title suitable for this view. This is a hint to the site view ad should 
     * reflect the current view input. 
     * @return the title or null if none
     */
    public String getTitle();
    
    /**
     * Get a icon suitable for this view. This is a hint to the view site and should 
     * reflect the current view input. 
     * @return the icon or null if none
     */
    public String getIcon();    
    
    /**
     * Refresh this view using current input. This should be called by a setInput after setting the input.
     * Optional operation - if container doesn't supports refresh do nothing
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
     * @return the input if any input or null if none
     */
    public Object getInput();
        
}
