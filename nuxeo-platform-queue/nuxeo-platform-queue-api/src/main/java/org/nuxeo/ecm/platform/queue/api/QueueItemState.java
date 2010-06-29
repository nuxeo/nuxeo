package org.nuxeo.ecm.platform.queue.api;
/*
 *
 */


// TODO: Auto-generated Javadoc
/**
 * An atomic item can be in the following state
 *
 * <dl>
 * <dt>Handled</dt>
 * <dd>content is currently handled by system</dd>
 * <dt>Orphaned</dt>
 * <dd>content known by system but not handled</dd>
 * </dl>
 * .
 *
 * @author "Stephane Lacoin at Nuxeo (aka matic)"
 */
public enum QueueItemState {

    /** The Handled. */
    Handled, /** The Orphaned. */
    Orphaned

}
