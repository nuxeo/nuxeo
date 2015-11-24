package org.nuxeo.opensocial.container.shared.layout.impl;

import java.io.Serializable;

/**
 * @author Stéphane Fourrier
 */
public class YUICustomBodySize extends YUIAbstractBodySize implements
        Serializable {
    private static final long serialVersionUID = 1L;

    private static final String CSS = "custom-doc";

    private long size;

    public YUICustomBodySize() {
    }

    /**
     * Constructeur de YUICustomBodySize
     *
     * @param size
     */
    public YUICustomBodySize(long size) {
        this.size = size;
    }

    /**
     * @see org.nuxeo.opensocial.container.shared.layout.impl.YUIAbstractBodySize.nuxeo.opensocial.layout.impl.core.YUIBodySize#getCSSClass()
     */
    @Override
    public String getCSS() {
        return CSS;
    }

    /**
     * @see org.nuxeo.opensocial.container.shared.layout.impl.YUIAbstractBodySize.nuxeo.opensocial.layout.impl.core.YUIBodySize#getSize()
     */
    @Override
    public long getSize() {
        return this.size;
    }

}
