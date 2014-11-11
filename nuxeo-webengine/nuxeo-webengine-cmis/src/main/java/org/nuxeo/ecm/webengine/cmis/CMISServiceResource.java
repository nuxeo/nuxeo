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
 */
package org.nuxeo.ecm.webengine.cmis;

import java.util.Map;

import javax.ws.rs.Consumes;

import org.apache.abdera.protocol.server.TargetType;
import org.apache.chemistry.atompub.CMIS;
import org.apache.chemistry.repository.Repository;
import org.nuxeo.ecm.core.chemistry.impl.NuxeoRepository;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.abdera.AbderaElementWriter;
import org.nuxeo.ecm.webengine.abdera.AbderaResponseWriter;
import org.nuxeo.ecm.webengine.atom.ServiceInfo;
import org.nuxeo.ecm.webengine.atom.ServiceResource;
import org.nuxeo.ecm.webengine.cmis.adapters.CMISCollection;
import org.nuxeo.ecm.webengine.cmis.adapters.EmptyCollection;
import org.nuxeo.ecm.webengine.cmis.adapters.ObjectChildrenCollection;
import org.nuxeo.ecm.webengine.cmis.adapters.TypesCollection;
import org.nuxeo.ecm.webengine.cmis.services.ServicesCollection;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.runtime.api.Framework;

/**
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@WebObject(type="cmis")
@Consumes({"application/atom+xml;type=entry", "*/*"})
public class CMISServiceResource extends ServiceResource {

    private static ServiceInfo info = null;

    public ServiceInfo getServiceInfo() {
        if (info == null) {
            synchronized (CMISServiceResource.class) {
                if (info == null) {
                    info = createServiceInfo();
                }
            }
        }
        return info;
    }
    
    public ServiceInfo createServiceInfo() {
        
        WebEngine engine =  Framework.getLocalService(WebEngine.class);
        engine.getRegistry().addMessageBodyWriter(new AbderaResponseWriter());
        engine.getRegistry().addMessageBodyWriter(new AbderaElementWriter());
        
        CMISServiceInfo service = new CMISServiceInfo();
        
        Repository repository = new NuxeoRepository("default");
        CMISWorkspaceInfo ws = new CMISWorkspaceInfo(repository, "default", "Nuxeo Repository");
        CMISCollectionInfo col = new CMISCollectionInfo("children", CMIS.COL_ROOT_CHILDREN, "Object Children", 
                new ObjectChildrenCollection("children", repository));
        col.setResourceType("ChildrenCollection");
        ws.addCollection(col);
        col = new CMISCollectionInfo("descendants", CMIS.COL_ROOT_DESCENDANTS, "Object Descendants", 
                new ObjectChildrenCollection("descendants", repository));
        col.setResourceType("ChildrenCollection");
        ws.addCollection(col);
        col = new CMISCollectionInfo("types_children", CMIS.COL_TYPES_CHILDREN, "Types Children", 
                new TypesCollection("types_children", repository));        
        ws.addCollection(col);
        col = new CMISCollectionInfo("types_descendants", CMIS.COL_TYPES_DESCENDANTS, "Types Descendants", 
                new TypesCollection("types_descendants", repository));
        ws.addCollection(col);
        col = new CMISCollectionInfo("unfiled", CMIS.COL_UNFILED, "Unfiled Objects",
                new EmptyCollection<Object>("unfiled", "Unfiled Objects", repository));
        col = new CMISCollectionInfo("checkedout", CMIS.COL_CHECKED_OUT, "Checkedout Objects",
                new EmptyCollection<Object>("checkedout", "Checkedout Objects", repository));
        col = new CMISCollectionInfo("query", CMIS.COL_QUERY, "Persisted Queries", 
                new EmptyCollection<Object>("query", "Persisted Queries", repository));
        ws.addCollection(col);

        col = new CMISCollectionInfo("services", "services", "Nuxeo Services", 
                new ServicesCollection("services", repository));
        ws.addCollection(col);

        service.addWorkspace(ws);
        
        return service;
    }

    @SuppressWarnings("unchecked")
    public String urlFor(WebContext ctx, Object key, Object params) {
        TargetType type = (TargetType)key;
        Map<String,String> args = (Map<String,String>)params; 
        if (type == TargetType.TYPE_ENTRY) {
            String rtype = args.get(CMISCollection.RESOURCE_TYPE);
            String rid = args.get(CMISCollection.RESOURCE_ID);
            if ("object".equals(rtype)) {
                return urlForObject(ctx, rid);
            } else if ("file".equals(rtype)) {
                return urlForFile(ctx, rid);
            } else if ("type".equals(rtype)) {
                return urlForType(ctx, rid);
            }
        } else if (type == TargetType.TYPE_COLLECTION) {                    
            String rtype = args.get(CMISCollection.RESOURCE_TYPE);
            String rid = args.get(CMISCollection.RESOURCE_ID);
            Resource rs = ctx.head().getNext().getNext(); // get the repository resource
            StringBuilder result = ctx.getServerURL().append(rs.getPath()).append("/").append(rtype);
            if (rid != null) {
                result.append("/").append(rid);
            }
            return result.toString();                    
        } else if (type == TargetType.TYPE_SERVICE) {
            return ctx.getServerURL().append(ctx.getModulePath()).toString();
        }
        throw new WebException("Cannot resolve URL for: "+key+" with args "+args, 500);
    }

    public static String urlForObject(WebContext ctx, String id) {
        Resource rs = ctx.head().getNext().getNext(); // get the repository resource
        return ctx.getServerURL().append(rs.getPath()).append("/objects/").append(id).toString();
    }

    public static String urlForType(WebContext ctx, String id) {
        Resource rs = ctx.head().getNext().getNext(); // get the repository resource
        return ctx.getServerURL().append(rs.getPath()).append("/types/").append(id).toString();
    }

    public static String urlForFile(WebContext ctx, String id) {
        Resource rs = ctx.head().getNext().getNext(); // get the repository resource
        return ctx.getServerURL().append(rs.getPath()).append("/files/").append(id).toString();
    }

    public static String urlForService(WebContext ctx, String id) {
        Resource rs = ctx.head().getNext().getNext(); // get the repository resource
        return ctx.getServerURL().append(rs.getPath()).append("/services/").append(id).toString();
    }

    
}
