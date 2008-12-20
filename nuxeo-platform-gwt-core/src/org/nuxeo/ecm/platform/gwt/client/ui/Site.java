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
 * Defines an site for a view. 
 * This is an abstract concept that helps to separate the logical part of managing views
 * from the widget management.
 * 
 * Widget frameworks must implement the {@link Container} class in order to be able to
 * interact with view sites.
 * 
 *  A site defines the place where the view will be placed. This 'place' can have a title and an icon.
 *  For example it can be a TAB in a tabbed view or a SECTION in an accordion view.
 *  
 *  When a site will open an input object the view will be initialized with that input and the container updated if 
 *  the title or icon changed. 
 *  
 *  A site is keeping a reference to the underlying data (that can be an widget or a data object) through the handle member.   
 *  The handle is created by the container when the site will be installed into the container.
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface Site {

    /**
     * Get the site name
     * @return the site name
     */
    String getName();
    
    /**
     * The handle is an object created by the underlying widget framework
     * and is used to associate a view site with a real widget.
     * The handle will be created by a {@link Container} object that is connected to the view manager.
     * The type of the handle is specific to each UI implementation  
     * @return the handle. can be null if the handle was not yet initialized
     */
    Object getHandle();
        
    /**
     * Get the view attached to that site.
     * @return the view.
     */
    View getView();
    
    /**
     * Get the title to be used for this site. This is generally asking the view to get a 
     * title based on the current input
     * @return the title or the site name if none
     */
    String getTitle();
    
    /**
     * get the icon to be used for that site. This is generally asking the view to get an icon based on the current input.
     * @return the icon or null if none
     */
    String getIcon();    
    
    /**
     * Open an input in that site. 
     * 
     * The connection to the container will be created if it is not yet existing. In that case the container will
     * create a handle for that site.
     * Then the view is asked if it is accepting the input. If not the container will be asked to hide this site.
     * If the view is accepting the input it will be installed in the container if it is not installed.
     * Then the view will have its input refreshed which may trigger UI updates like view content, title or icon. 
     * The site is remembering its container so that methods like {@link #enable()} or {@link #activate()} 
     * can be called later.
     * @param input
     */
    void open(Container container, Object input);    

    /**
     * Ask the site to update its title based on its view title.
     * This will trigger a {@link View#getTitle()} operation
     */
    void updateTitle();
    
    /**
     * Ask the site to update its icon based on its view icon.
     * This will trigger a {@link View#getIcon()} operation 
     */
    void updateIcon();
    
    /**
     * Ask the site to update its widget based on its view  widget.
     * This will trigger a {@link View#getWidget()} operation
     */
    void updateWidget();
    
    /**
     * Enable this site. This will ask the container to enable the site.
     * How enablement affect widget state is up to the container implementation.
     * The container may show/hide the widget or may disable/enable it
     */
    void enable();

    /**
     * Disable this site. This will ask the container to disable the site.
     * How enablement affect widget state is up to the container implementation.
     * The container may show/hide the widget or may disable/enable it
     */
    void disable();    
    
    /**
     * Activate this site. This will ask the container to activate the site.
     * Activating means selecting the site. In a tab view it means selecting the site tab, 
     * in an accordion view it means expanding the site section.  
     */
    void activate();

    /**
     * Deactivate this site. This will ask the container to deactivate the site.
     * Deactivating means deselecting the site. In a tab view it means deselecting the site tab, 
     * in an accordion view it means collapsing the site section.  
     */
    void deactivate();
    
    /**
     * Close this site. After a site was closed it cannot be reused. (a new one will be created)
     * This can be used for example in tab views that support closeable tabs.
     */
    void close();
    
    /**
     * Whether or not this view is activated
     * @return
     */
    boolean isActive();
    
    /**
     * Whether or not this site is enabled. 
     * @return
     */
    boolean isEnabled();
    
}
