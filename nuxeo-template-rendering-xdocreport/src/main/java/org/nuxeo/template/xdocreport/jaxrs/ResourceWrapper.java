package org.nuxeo.template.xdocreport.jaxrs;

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
        rs.setType(ResourceType.TEMPLATE);
        try {
            rs.setName(srcDocument.getName());
            rs.setId(srcDocument.getId());

            Resource fileResource = new NonRecursiveResource();
            fileResource.setName(srcDocument.getFileName());
            fileResource.setId(srcDocument.getId());
            fileResource.setType(ResourceType.DOCUMENT);

            Resource METAResource = new NonRecursiveResource();
            METAResource.setName("META-INF");
            METAResource.setId(srcDocument.getId() + "/META-INF");
            METAResource.setType(ResourceType.CATEGORY);

            Resource fieldResource = new NonRecursiveResource();
            fieldResource.setName(srcDocument.getName() + ".fields.xml");
            fieldResource.setId(srcDocument.getId() + ".fields.xml");
            fieldResource.setType(ResourceType.DOCUMENT);

            METAResource.getChildren().add(fieldResource);

            rs.getChildren().add(fileResource);
            rs.getChildren().add(METAResource);

        } catch (ClientException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        // rs.getChildren().addAll(new ArrayList<Resource>());
        return rs;
    }
}
