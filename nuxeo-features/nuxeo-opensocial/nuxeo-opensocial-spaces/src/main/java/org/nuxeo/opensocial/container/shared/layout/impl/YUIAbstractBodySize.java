package org.nuxeo.opensocial.container.shared.layout.impl;

import java.io.Serializable;

import org.nuxeo.opensocial.container.shared.layout.api.YUIBodySize;

/**
 * @author Stéphane Fourrier
 */
public abstract class YUIAbstractBodySize implements Serializable, YUIBodySize {
    private static final long serialVersionUID = 1L;

    /**
     * Retourne la taille en pixel
     *
     * @return size
     */
    public abstract long getSize();

    /**
     * Retourne la classe CSS
     *
     * @return CSSClass
     */
    public abstract String getCSS();
}
