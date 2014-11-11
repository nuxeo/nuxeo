/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.ecm.platform.queue.api;

/**
 * Atomic content service provide long processing operation management such as OCRing a
 * document, manipulation images and so on, sometimes one operation
 * after the other.
 * <p>
 * The atomic content service goals is to:
 * <ul>
 * <li>create a content handling queues to which we can add content to be processed</li>
 * <li>monitor what contents are in the queue</li>
 * <li>being able to restart a content handling after the server is back on. (If
 *   you have a long process that goes from queue1 to queue2 to queue
 *   3, and the server is shutdown while in queue3, you should be able to
 *   replay queue3 only)</li>
 * <li>being able to query the queue so to not send a job if it is
 *   already in the queue</li>
 * </ul>
 * A long operation is some CPU cycles to be run. It will (usually) process some
 * content. The content has all the necessary information for the operation being handled.
 * A content should provide an id that uniquely identify it inside regarding it's queue.
 * <p>
 * To define a queue, we provide (using contribution to EP):
 * <ul>
 * <li>an atomic content descriptor (a class with handle methods)</li>
 * <li>an atomic content persister</li>
 * </ul>
 * When we need to handle a content, we get the queue associated to and give it the content to be handled.
 * The queue will persist the content using the persister and ask the content class for handling self.
 * Part of the content handling should be to ask queue to forget content. It can
 * also generate another content for another queue if needed. Altering the content queue is always done in a
 * dedicated transaction.
 * <p>
 * <img src="doc-files/handle-content-if-unknown.jpg"/>
 * A queue is able to give the list of content that
 * are currently being handled (the list of job currently using CPU is irrelevant).
 * <p>
 * We will provide 3 implementations of content persister :
 * <ul>
 * <li> repository based </li>
 * <li> in memory (for test) </li>
 * <li> filesystem based </li>
 * </ul>
 *
 * The content persister is able to list the content currently in its queue with some
 * administration information (when was it added, by who ...)
 * If there is a server crash, the content  stays persisted. When the server restart,
 * The queue does not automatically recover the content from the queue. These content are said orphaned.
 * They are known by the persister but not in the current list of handled content.
 * <p>
 * By design, we don't want a queue to pick up orphaned job automatically because, for some reason,
 * this job could be the reason of the server crash. We will add to the service a simple UI to list the different
 * queues, the jobs being handled and the orphaned jobs. It would also be possible to recover orphaned content manually.
 * An orphaned content can be handled on any server in the cluster.
 * <p>
 * The service would offer ways to:
 * <ul>
 * <li> get a queue
 * <li>handle a new content (do not make use of shared locks)</li>
 * <li>handle a content if not already known</li>
 * <li>query queues for content (contains predicate, list handled content, list orphaned content)</li>
 * <li>forget content</li>
 * </ul>
 * <p>
 * It is the authority of the content handling  to remove contents from queues.
 * It might decides to keep the list of all jobs done.
 */
