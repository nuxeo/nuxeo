package org.nuxeo.ecm.platform.web.common.requestcontroller.service;

import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.platform.web.common.requestcontroller.filter.NuxeoRequestControllerFilter;

/**
 * Interface for the {@link NuxeoRequestControllerFilter} config
 * for a given {@link HttpServletRequest}.
 *
 * @author tiry
 */
public interface RequestFilterConfig  extends Serializable{

    boolean needSynchronization();

    boolean needTransaction();

}
