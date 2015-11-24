package org.nuxeo.opensocial.container.shared.layout.impl;

import java.io.Serializable;

import org.nuxeo.opensocial.container.shared.layout.enume.YUISize;

/**
 * @author Stéphane Fourrier
 */
public class YUIFixedBodySize extends YUIAbstractBodySize implements
        Serializable {
    private static final long serialVersionUID = 1L;

    private YUISize size;

    public YUIFixedBodySize() {
    }

    /***
     * Constructeur de YUIFixedBodySize
     *
     * @param size
     */
    public YUIFixedBodySize(YUISize size) {
        this.size = size;
    }

    /**
     * @see org.nuxeo.opensocial.container.shared.layout.impl.YUIAbstractBodySize.nuxeo.opensocial.layout.impl.core.YUIBodySize#getCSSClass()
     */
    @Override
    public String getCSS() {
        return size.getCSS();
    }

    /**
     * @see org.nuxeo.opensocial.container.shared.layout.impl.YUIAbstractBodySize.nuxeo.opensocial.layout.impl.core.YUIBodySize#getSize()
     */
    @Override
    public long getSize() {
        return size.getSize();
    }
}
