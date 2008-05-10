
msg="Your document has been created successfully."
Response.sendRedirect("${Context.getLastResolvedObject().getAbsolutePath()}?msg=${msg}")
