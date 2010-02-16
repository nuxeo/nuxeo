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

package org.nuxeo.apidoc.browse;

import java.util.List;

import javax.transaction.UserTransaction;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.apidoc.snapshot.DistributionSnapshot;
import org.nuxeo.apidoc.snapshot.SnapshotManager;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * @author <a href="mailto:td@nuxeo.com">Thierry Delprat</a>
 *
 */
@WebObject(type = "distribution")
public class Distribution extends ModuleRoot{
	
	protected static Log log = LogFactory.getLog(Distribution.class);
	
    @GET
    @Produces("text/html")
    public Object doGet() {
        return getView("index");        
    }
        
    @Path(value = "{distributionId}")
    public Resource viewDistribution(@PathParam("distributionId") String distributionId) {
        try {
        	if (distributionId==null || "".equals(distributionId)) {
        		return this;
        	}
        	ctx.setProperty("distribution", SnapshotManager.getSnapshot(distributionId,ctx.getCoreSession()));
        	ctx.setProperty("distId", distributionId);
            return ctx.newObject("apibrowser", distributionId);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }
    }
    
    public List<String> getAvailableDistributions() {    	
    	return SnapshotManager.getAvailableDistributions(ctx.getCoreSession());    
    }
    
    public String getRuntimeDistributionName() {    	
    	return SnapshotManager.RUNTIME;
    }

    public DistributionSnapshot getRuntimeDistribution() {    	
    	return SnapshotManager.getRuntimeSnapshot();
    }

    public List<String> getPersistedDistributions() {    	
    	return SnapshotManager.getPersistentSnapshotNames(ctx.getCoreSession());    
    }
    
    public DistributionSnapshot getCurrentDistribution() {
    	String distId = (String) ctx.getProperty("distId");
    	if (distId==null) {
    		return null;
    	} else {
    		return SnapshotManager.getSnapshot(distId,ctx.getCoreSession());
    	}
    }
    
    @POST
    @Path(value = "save")    
    public Object doSave() throws Exception {
    	log.info("Start Snapshoting...");
    	UserTransaction tx = TransactionHelper.lookupUserTransaction(); 
    	if (tx!=null) {
    		tx.begin();
    	}
    	try {
    		SnapshotManager.getRuntimeSnapshot().persist(getContext().getCoreSession());
    	}
    	catch (Exception e) {
    		log.error("Error during storage", e);	
    		if (tx!=null) {
    			tx.rollback();
    		}
    		return getView("index");
		}
    	log.info("Snapshoting saved.");
		if (tx!=null) {
			tx.commit();
		}
		return getView("index");    	    	    	
    }
}
