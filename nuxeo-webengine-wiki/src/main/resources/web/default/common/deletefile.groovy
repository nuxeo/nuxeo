
msg="The file has been deleted."
Response.sendRedirect("${req.getLastResolvedObject().getAbsolutePath()}?msg=${msg}")
