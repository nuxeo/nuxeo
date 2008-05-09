response = req.getResponse()
response.sendRedirect("${req.getLastResolvedObject().getAbsolutePath()}/FrontPage")
