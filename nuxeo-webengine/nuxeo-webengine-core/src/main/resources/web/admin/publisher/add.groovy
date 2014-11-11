
form = Context.form;
appId = form.getString("webapp");
path = form.getString("path");
docPath = form.getString("docpath");
docMapper = Engine.documentMapper;

docMapper.addMapping(appId, path, docPath);
try {
  docMapper.store();
} catch (e) {
  Response.writer("Failed to store mappings");
  e.printStackTrace(Response.writer);
}

Context.redirect(This.urlPath);
