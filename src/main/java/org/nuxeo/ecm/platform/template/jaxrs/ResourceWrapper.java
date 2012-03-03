package org.nuxeo.ecm.platform.template.jaxrs;

import java.util.ArrayList;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.platform.template.adapters.source.TemplateSourceDocument;

import fr.opensagres.xdocreport.remoting.resources.domain.Resource;

public class ResourceWrapper {
    
    public static Resource wrap(TemplateSourceDocument srcDocument) {
        Resource rs = new Resource();
        rs.setType(Resource.FILE_TYPE);      
        try {
            rs.setName(srcDocument.getFileName());
        } catch (ClientException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        rs.setChildren(new ArrayList<Resource>());
        return rs;        
    }
}
