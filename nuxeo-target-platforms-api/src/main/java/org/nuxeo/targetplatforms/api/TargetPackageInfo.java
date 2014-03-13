/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/).
 * This is unpublished proprietary source code of Nuxeo SA. All rights reserved.
 * Notice of copyright on this source code does not indicate publication.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.api;

import java.util.List;

/**
 * Represents a target package info, useful for listing of available target
 * packages.
 *
 * @since 2.18
 */
public interface TargetPackageInfo extends TargetInfo {

    /**
     * Returns the list of target packages that this target package depends on.
     */
    List<String> getDependencies();

}
