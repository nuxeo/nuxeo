
msg="The document has been updated successfully."
Response.sendRedirect("${req.getLastResolvedObject().getAbsolutePath()}?msg=${msg}")
