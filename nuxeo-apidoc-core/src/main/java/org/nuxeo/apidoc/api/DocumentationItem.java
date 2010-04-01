package org.nuxeo.apidoc.api;

import java.util.List;
import java.util.Map;

public interface DocumentationItem {

    /**
     * get Title of the Documentation
     *
     * @return
     */
    String getTitle();

    /**
     * get the main content of the Documentation
     *
     * @return
     */
    String getContent();

    /**
     * get the type of the documentation (description, how-to, samples ...)
     *
     * @return
     */
    String getType();

    /**
     * get the output rendering format for the content (wiki, html ...)
     * @return
     */
    String getRenderingType();

    /**
     * get label for the documentation type
     * @return
     */
    String getTypeLabel();

    /**
     * get versions
     * @return
     */
    List<String> getApplicableVersion();

    /**
     * get identifier of the target documented artifact
     *
     * @return
     */
    String getTarget();

    /**
     * get the Type of the target documented artifact (NXBundle, NXComponent ...)
     * @return
     */
    String getTargetType();

    /**
     * Indicates if documentation has been validated by Nuxeo
     *
     * @return
     */
    boolean isApproved();

    /**
     * Local documentation identifier
     *
     * @return
     */
    String getId();

    /**
     * UUID of the underlying DocumentModel
     *
     * @return
     */
    String getUUID();

    /**
     * returns attachements
     * @return
     */
    Map<String, String> getAttachements();

}
