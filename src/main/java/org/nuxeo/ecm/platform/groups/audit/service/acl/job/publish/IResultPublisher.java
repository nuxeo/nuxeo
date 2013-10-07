package org.nuxeo.ecm.platform.groups.audit.service.acl.job.publish;

import java.io.Serializable;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;

public interface IResultPublisher extends Serializable {

    public void publish(FileBlob fileBlob) throws ClientException;

}
