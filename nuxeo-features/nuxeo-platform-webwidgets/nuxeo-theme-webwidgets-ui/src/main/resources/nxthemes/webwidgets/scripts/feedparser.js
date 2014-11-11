var FeedParser = Class.create();
FeedParser.prototype = {

  initialize : function(data) {
    this.data = data
  },

  parse : function() {
    var xotree = new XML.ObjTree();
    var tree = xotree.parseXML(this.data);
    this.data = tree;

    var version = this.guessVersion();
    switch(version) {
      case "rss10":
        parser = new FeedParser.rss10();
        break;
      case "rss20":
        parser = new FeedParser.rss20();
        break;
      case "atom":
        parser = new FeedParser.atom();
        break;
    }

    parser.data = this.data;
    parser.version = version;

    this.version = version;
    this.type = parser.getType();
    this.title = parser.getTitle();
    this.content = parser.getDescription();
    this.htmlUrl = parser.getLink();
    this.items = parser.getItems();
  },

  guessVersion : function() {
    var version;
    if (this.data['rdf:RDF'] &&
    this.data['rdf:RDF']['-xmlns'] == 'http://purl.org/rss/1.0/') {
      version = 'rss10';
    } else if (this.data['rss'] && this.data['rss']['-version'] == '2.0') {
      version = 'rss20';
    } else if (this.data['feed'] && this.data['feed']['-xmlns'] == 'http://www.w3.org/2005/Atom') {
      version = 'atom';
    } else {
      version = null;
    }
    return version;
  }

};

FeedParser.rss10 = Class.create();
FeedParser.rss10.prototype = {

  initialize : function() {},

  getType: function() {
    return 'rss';
  },

  getTitle : function() {
    return this.data['rdf:RDF'].channel.title;
  },

  getLink : function() {
    return this.data['rdf:RDF'].channel.link;
  },

  getDescription: function() {
    return this.data['rdf:RDF'].channel.description;
  },

  getItems : function() {
    var items = this.data['rdf:RDF'].item;
    var res = [];
    items.each(function(item) {
      item['date'] = item['date'] || item['dc:date'];
      item['content'] = item['description'];
      res.push(item);
    });
    return res;
  }
};

FeedParser.rss20 = Class.create();
FeedParser.rss20.prototype = {

  initialize : function() {},

  getType: function() {
    return 'rss';
  },

  getTitle : function() {
    return this.data['rss'].channel.title;
  },

  getLink : function() {
    return this.data['rss'].channel.link;
  },

  getDescription: function() {
    return this.data['rss'].channel.description;
  },

  getItems : function() {
    var items = this.data['rss'].channel.item;
    var res = [];
    items.each(function(item) {
      item['date'] = item['date'] || item['dc:date'] || item['pubDate'];
      item['content'] = item['description'];
      res.push(item);
    });
    return res;
  }

};

FeedParser.atom = Class.create();
FeedParser.atom.prototype = {

  initialize : function() {},

  getType: function() {
    return 'atom';
  },

  getTitle : function() {
    var title =  this.data['feed'].title;
    if (typeof(title) == 'string') {
      return title;
    } else {
      return title['#text'];
    }
  },

  getLink : function() {
    var link = this.data['feed'].link;
    var res = '';
    if (!link['-href']) {
      link.each(function(ln) {
        if (ln['-rel'] == 'alternate') {
          res = ln['-href'];
        }
      });
    } else {
      res = this.data['feed'].link['-href'];
    }
      return res;
  },

  getDescription: function() {
    return this.data['feed'].subtitle;
  },

  getItems : function() {
    var items = this.data['feed'].entry;
    var res = [];
    items.each(function(item) {
      var title = item['title'] = item.title;
      if (typeof(title) == 'string') {
        item['title'] = title;
      } else {
        item['title'] = title['#text'] || title['#cdata-section'];
      }
      item['date'] = item['date'] || item['dc:date'] || item['created'];
      if (typeof(item.link['-href']) != 'string') {
        item.link.each(function(ln) {
          if (ln['-rel'] == 'alternate') {
            item['link'] = ln['-href'];
          }
        });
      } else {
        item['link'] = item.link['-href'] || '';
      }
      item['content'] = item.summary['#text'] || item.summary['#cdata-section'];
      res.push(item);
    });
    return res;
  }
};