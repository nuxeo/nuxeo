
msg="The document has been updated successfully."
Response.sendRedirect("${Context.getLastResolvedObject().getAbsolutePath()}?msg=${msg}")
