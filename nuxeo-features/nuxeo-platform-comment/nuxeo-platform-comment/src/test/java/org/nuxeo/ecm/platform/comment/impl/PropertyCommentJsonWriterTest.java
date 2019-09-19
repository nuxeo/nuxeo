package org.nuxeo.ecm.platform.comment.impl;

import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.runtime.test.runner.Deploy;

/**
 * @since 11.1
 */
@Deploy("org.nuxeo.ecm.platform.comment.tests:OSGI-INF/property-comment-manager-override.xml")
public class PropertyCommentJsonWriterTest extends AbstractCommentJsonWriterTest {

    @Override
    protected Class<? extends CommentManager> getCommentManager() {
        return PropertyCommentManager.class;
    }
}
