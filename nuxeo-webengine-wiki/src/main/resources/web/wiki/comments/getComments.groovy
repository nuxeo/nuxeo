import org.nuxeo.ecm.platform.comment.api.CommentableDocument
cDoc = Document.getAdapter(org.nuxeo.ecm.platform.comment.api.CommentableDocument);

comments = cDoc.getComments()

Context.render("WikiPage/show_comments.ftl", ['comments': comments])