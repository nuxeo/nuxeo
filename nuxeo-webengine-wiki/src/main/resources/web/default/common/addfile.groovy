
msg="The file has been attached."
Response.sendRedirect("${req.getLastResolvedObject().getAbsolutePath()}?msg=${msg}")
