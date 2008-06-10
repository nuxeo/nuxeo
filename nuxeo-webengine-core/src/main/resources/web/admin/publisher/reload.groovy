
docMapper = Engine.getDocumentMapper();

try {
  docMapper.load();
} catch (e) {
  Response.writer("Failed to load mappings");
  e.printStackTrace(Response.writer);
}    

Context.redirect(This.urlPath);
