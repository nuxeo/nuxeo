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
 *
 * $Id$
 */

package org.nuxeo.runtime.osgi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;


/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class OSGiComponentLoader implements SynchronousBundleListener {

    private static final Log log = LogFactory.getLog(OSGiComponentLoader.class);
    private static final Log componentDebugLog = LogFactory.getLog("nuxeo.bundle.debug");

    private final OSGiRuntimeService runtime;

    public OSGiComponentLoader(OSGiRuntimeService runtime) {
        this.runtime = runtime;
        install();
    }

    public void install() {
        BundleContext ctx = runtime.getBundleContext();
        ctx.addBundleListener(this);
        Bundle[] bundles = ctx.getBundles();
        int mask = Bundle.RESOLVED | Bundle.STARTING | Bundle.ACTIVE;
        for (Bundle bundle : bundles) {
        	String name=bundle.getSymbolicName();
        	boolean isNuxeo=isNuxeoBundle(name);
            int state = bundle.getState();
            
            bundleDebug("install : "+name+" = "+bundleStateAsString(state),
            		isNuxeo);
            if ((state & mask) != 0) { // check only resolved bundles
                if (OSGiRuntimeService.getComponentsList(bundle) != null) {
                    bundleDebug("install : "+name+" component list:"+
                    		OSGiRuntimeService.getComponentsList(bundle),isNuxeo);
                    // check only bundles containing nuxeo comp.
                    try {
                        runtime.createContext(bundle);
                    } catch (Throwable e) {
                        log.warn("Failed to load components for bundle - "
                                + bundle.getSymbolicName(),e);
                    }
                } else {
                    bundleDebug("install : bundle : "+name+" has no components",
                    		isNuxeo);
                }
            } else {
                bundleDebug("install : bundle : "+name+
                		" is not RESOLVED, STARTING, or ACTIVE, so no context" +
                		" was created",isNuxeo);
            }
        }
    }

    public void uninstall() {
        runtime.getBundleContext().removeBundleListener(this);
    }

    public void bundleChanged(BundleEvent event) {
    	String name=event.getBundle().getSymbolicName();
    	boolean isNuxeo=isNuxeoBundle(name);
    	
        bundleDebug("bundle changed: "+
        		name+" event = "+
        		bundleEventAsString(event.getType()), isNuxeo);
        try {
            Bundle bundle = event.getBundle();
            int type = event.getType();
            switch (type) {
            case BundleEvent.RESOLVED:
                if (OSGiRuntimeService.getComponentsList(bundle) != null) {
                	bundleDebug(name+
                			" : RESOLVED with components "+
                			OSGiRuntimeService.getComponentsList(bundle),
                			isNuxeo);
                    runtime.createContext(bundle);
                } else {
                	bundleDebug(name+
                			" : RESOLVED but no components",isNuxeo);
                }
                break;
            case BundleEvent.UNRESOLVED:
                if (OSGiRuntimeService.getComponentsList(bundle) != null) {
                	bundleDebug(name+
                			": UNRESOLVED with components "+
                			OSGiRuntimeService.getComponentsList(bundle),
                			isNuxeo);
                    runtime.destroyContext(bundle);
                } else {
                	bundleDebug(name+
                			": UNRESOLVED but no component list",isNuxeo);
                }
                break;
            }
        } catch (Exception e) {
            log.error(e);
        }
    }

    /**
     * Used for generating good debug info. Convert bit vector into printable
     * string.
     * @param int state bitwise-or of UNINSTALLED, INSTALLED, RESOLVED, STARTING, 
     * STOPPING, and ACTIVE
     * @return printable version of bits that are on 
     */
    public static String bundleStateAsString(int state) {
    	StringBuffer buff=new StringBuffer();
    	boolean first=true;
    	
    	buff.append("[");
    	if ((state & Bundle.UNINSTALLED) != 0) {
    		buff.append("UNINSTALLED");
    		first=false;
    	}
    	if ((state & Bundle.INSTALLED) != 0) {
    		if (!first) {
    			buff.append(",");
    		}
    		buff.append("INSTALLED");
    		first=false;
    	}
    	if ((state & Bundle.RESOLVED) != 0) {
    		if (!first) {
    			buff.append(",");
    		}
    		buff.append("RESOLVED");
    		first=false;
    	}
    	if ((state & Bundle.STARTING) != 0) {
    		if (!first) {
    			buff.append(",");
    		}
    		buff.append("STARTING");
    		first=false;
    	}
    	if ((state & Bundle.STOPPING) != 0) {
    		if (!first) {
    			buff.append(",");
    		}
    		buff.append("STOPPING");
    		first=false;
    	}
    	if ((state & Bundle.ACTIVE) != 0) {
    		if (!first) {
    			buff.append(",");
    		}
    		buff.append("ACTIVE");
    		first=false;
    	}
    	buff.append("]");
    	return buff.toString();
    }
    /**
     * Used for generating good debug info. Convert event type into printable
     * string.
     * @param int INSTALLED, STARTED,STOPPED, UNINSTALLED,UPDATED
     * @return printable version of event type
     */
    public static String bundleEventAsString(int eventType) {
    	switch (eventType) {
    	case BundleEvent.INSTALLED:
    		return "INSTALLED";
    	case BundleEvent.STARTED:
    		return "STARTED";
    	case BundleEvent.STARTING:
    		return "STARTING";
    	case BundleEvent.STOPPED:
    		return "STOPPED";
    	case BundleEvent.UNINSTALLED:
    		return "UNINSTALLED";
    	case BundleEvent.UPDATED:
    		return "UPDATED";
    	case BundleEvent.LAZY_ACTIVATION:
    		return "LAZY_ACTIVATION";
    	case BundleEvent.RESOLVED:
    		return "RESOLVED";
    	case BundleEvent.UNRESOLVED:
    		return "UNRESOLVED";
    	case BundleEvent.STOPPING:
    		return "STOPPING";
    	default:
    		return "UNKNOWN OSGI EVENT TYPE ("+eventType+")!";
    	}
    }
    
    public static boolean isNuxeoBundle(String symbolicName) {
    	if (symbolicName.startsWith("org.nuxeo")) {
    		return true;
    	}
    	if (symbolicName.equals("osgi.core")) {
    		return true;
    	}
    	//oh, how I hate all the stupid logging crap we have to do... couldn't
    	//sun have just gotten this right in version 1.0 and prevented a lot
    	//of problems? there are now 2 (TWO!) apache meta-logging frameworks!
    	if (symbolicName.startsWith("slf4j")) {
    		return true;
    	}
    	
    	//everything else is not nuxeo
    	return false;
    }
    
    /**
     * Print out a debug message for debugging bundles.  
     * @param String msg the debug message
     * @param boolean isNuxeo true if this is a "built-in" bundle that ships
     *   with the system
     *   
     */
    public static void bundleDebug(String msg, boolean isNuxeo) {
    	if (isNuxeo) {
    		componentDebugLog.debug(msg);
    	} else {
    		componentDebugLog.info(msg);
    	}
    }

    /**
     * Print out a debug message for debugging bundles.  
     * @param String msg the debug message
     */
    public static void bundleDebug(String msg) {
    	bundleDebug(msg,false);
    }
}
