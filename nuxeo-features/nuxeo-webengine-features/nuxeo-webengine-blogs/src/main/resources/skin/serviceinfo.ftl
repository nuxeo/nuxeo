<?xml version="1.0" encoding="utf-8"?>
<service xmlns="http://www.w3.org/2007/app"
         xmlns:atom="http://www.w3.org/2005/Atom">
  <workspace>
    <atom:title>${blog.title}</atom:title>
    <collection
        href="${blog.appUrl}" >
      <atom:title>My Blog Entries</atom:title>
    </collection>
  </workspace>
</service>
