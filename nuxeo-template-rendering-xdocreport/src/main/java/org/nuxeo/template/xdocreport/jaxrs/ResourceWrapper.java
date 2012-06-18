package org.nuxeo.template.xdocreport.jaxrs;

import java.util.ArrayList;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.template.api.adapters.TemplateSourceDocument;

import fr.opensagres.xdocreport.remoting.resources.domain.Resource;
import fr.opensagres.xdocreport.remoting.resources.domain.ResourceType;

/**
 * 
 * @author <a href="mailto:tdelprat@nuxeo.com">Tiry</a>
 * 
 */
public class ResourceWrapper {

    public static Resource wrap(TemplateSourceDocument srcDocument) {
        Resource rs = new Resource();
        rs.setType(ResourceType.FILE);
        try {
            rs.setName(srcDocument.getFileName());
            rs.setId(srcDocument.getId());
        } catch (ClientException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        rs.getChildren().addAll(new ArrayList<Resource>());
        return rs;
    }
}
