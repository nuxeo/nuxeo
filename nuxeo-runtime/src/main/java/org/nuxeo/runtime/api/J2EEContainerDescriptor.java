/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.runtime.api;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.Environment;

/**
 * Helper class to get container related properties
 *
 * @author Stephane Lacoin
 * @author bstefanescu
 */
public enum J2EEContainerDescriptor {

	JBOSS("java:", TransactionTypeHelper.JTA), 
	TOMCAT("java:comp/env/jdbc", TransactionTypeHelper.RESOURCE_LOCAL),
	JETTY("jdbc", TransactionTypeHelper.RESOURCE_LOCAL), 
	GF3("jdbc", TransactionTypeHelper.RESOURCE_LOCAL);	

	private J2EEContainerDescriptor(String datasourcePrefix, String hibernateTransactionStrategy) {
		this.datasourcePrefix = datasourcePrefix;
		this.txFactory = hibernateTransactionStrategy;
	}

	public final String datasourcePrefix;

	public final String txFactory;

	public static final Log log = LogFactory.getLog(J2EEContainerDescriptor.class);

	protected static J2EEContainerDescriptor autodetect() {
	    Environment env = Environment.getDefault();

	    String hostName = env.getHostApplicationName();
	    if (hostName == null) {
	        return null;
	    }
	    
	    if (Environment.JBOSS_HOST.equals(hostName)) {
	        log.info("Detected JBoss host");
	        return JBOSS;
	    } else if (Environment.TOMCAT_HOST.equals(hostName)) {
	        log.info("Detected Tomcat host");
	        return TOMCAT;
	    } else if (Environment.NXSERVER_HOST.equals(hostName)) {
	        // can be jetty or gf3 embedded
	        try {
	            Class.forName("com.sun.enterprise.glassfish.bootstrap.AbstractMain");
	            log.info("Detected GlassFish host");
	            return GF3;
	        } catch (Exception e) {
	            log.debug("Autodetect : not a glassfish host");
	        }
	        try {
	            Class.forName("org.mortbay.jetty.webapp.WebAppContext");
	            log.info("Detected Jetty host");
	            return JETTY;
	        } catch (Exception e) {
	            log.debug("Autodetect : not a jetty host");
	        }
	        return null; // unknown host
	    } else {
	        return null; // unknown host
	    }		
	}
	
	private static J2EEContainerDescriptor selected;

    /**
     * Sets the prefix to be used (mainly for tests).
     */
	public static void setSelected(J2EEContainerDescriptor selected) {
		J2EEContainerDescriptor.selected = selected;
	}
	
	public static J2EEContainerDescriptor getSelected() {
		 if (selected != null) {
			 return selected;
		 }
		 return selected = autodetect();
	}

}
