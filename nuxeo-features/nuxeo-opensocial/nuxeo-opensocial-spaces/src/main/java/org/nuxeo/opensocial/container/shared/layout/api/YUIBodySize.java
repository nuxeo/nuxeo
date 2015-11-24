package org.nuxeo.opensocial.container.shared.layout.api;

import java.io.Serializable;

/**
 * @author Stéphane Fourrier
 */
public interface YUIBodySize extends Serializable {

    public long getSize();

    public String getCSS();

}
