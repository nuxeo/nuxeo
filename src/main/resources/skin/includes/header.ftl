<header>
  
  <h2>${docFolder.title}</h2>

  <detail>Shared by
    <a title="email address" href="mailto:${docFolder.easysharefolder.contactEmail}">${docFolder.easysharefolder.contactEmail}</a>
  </detail>

  <detail>
    Expiration Date : ${docFolder.dublincore.expired?string("dd/MM/yyyy")}
  </detail>

</header>