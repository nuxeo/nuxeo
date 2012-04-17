package org.nuxeo.template.jaxrs;

import java.util.ArrayList;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

import fr.opensagres.xdocreport.remoting.resources.domain.Resource;

/**
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * 
 */
public class ResourceWrapper {

    public static Resource wrap(TemplateSourceDocument srcDocument) {
        Resource rs = new Resource();
        rs.setType(Resource.FILE_TYPE);
        try {
            rs.setName(srcDocument.getFileName());
            rs.setId(srcDocument.getId());
        } catch (ClientException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        rs.setChildren(new ArrayList<Resource>());
        return rs;
    }
}
