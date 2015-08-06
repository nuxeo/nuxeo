package org.nuxeo.runtime.model;

import java.util.Properties;
import java.util.Set;


/**
 * @author Stephane Lacoin at Nuxeo (aka matic)
 * @since 7.4
 */
public interface Parameters {

    Properties getProperties();

    Set<Parameters.Descriptor> getDescriptors();

    interface Descriptor {

        String getName();

        String getDocumentation();

        String getValue();

    }

}