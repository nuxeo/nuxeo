
form = Context.getForm();
appId = form.getString("webapp");
path = form.getString("path");
docPath = form.getString("docpath");
docMapper = Engine.getDocumentMapper();

docMapper.addMapping(appId, path, docPath);
try {
  docMapper.store();
} catch (e) {
  Response.writer("Failed to store mappings");
  e.printStackTrace(Response.writer);
}

Context.redirect(This.urlPath);
