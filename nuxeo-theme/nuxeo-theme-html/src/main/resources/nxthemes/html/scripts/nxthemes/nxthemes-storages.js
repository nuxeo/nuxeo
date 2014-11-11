/*

 NXThemes UI library - storages

 Author: Jean-Marc Orliaguet <jmo@chalmers.se>

*/

NXThemes.registerStorages({

  local: function(model) {
    return new NXThemes.LocalStorage(model);
  },

  remote: function(model) {
    return new NXThemes.RemoteStorage(model);
  },

  unified: function(model) {
    return new NXThemes.UnifiedStorage(model);
  }

});


NXThemes.LocalStorage = Class.create();
NXThemes.LocalStorage.prototype = Object.extend(
  new NXThemes.StorageAdapter(), {

  setup: function() {
    this.cookie_name = 'nxthemes_local_storage_' + this.model.hash();
  },

  requestData: function() {
    var data = NXThemes.getSessionData(this.cookie_name);
    if (data != null) {
      this.write(data);
    } else {
      return;
    }
  },

  storeData: function(data) {
    var stored_data = this.write(data);
    NXThemes.setSessionData(this.cookie_name, stored_data);
  }

});

NXThemes.RemoteStorage = Class.create();
NXThemes.RemoteStorage.prototype = Object.extend(
  new NXThemes.StorageAdapter(), {

  requestData: function() {
    var model = this.model;
    var storage = this;

    var url = model.def.storage.accessors.get;
    if (!url) return;
    var options = {
      onComplete: function(req) {
        var content_type = req.getResponseHeader('content-type');
        if (content_type.match(/^text\/x-json/)) {
          var data = req.responseText.evalJSON(true);
          storage.write(data);
        }
      }
    }
    var parts = url.split('?');
    if (parts.length == 2) {
      url = parts[0];
      options.parameters = parts[1];
    }
    new Ajax.Request(url, options);
  },

  storeData: function(data) {
    var model = this.model;
    var storage = this;

    var url = model.def.storage.accessors.set;
    if (!url) return;
    new Ajax.Request(url, {
      method: "post",
      parameters: $H({
        "data": Object.toJSON(data)
        }).toQueryString(),
      onComplete: function(req) {
        var content_type = req.getResponseHeader('content-type');
        if (content_type.match(/^text\/x-json/)) {
          var data = req.responseText.evalJSON(true);
          storage.write(data);
        }
      }
    });
  }

})

NXThemes.UnifiedStorage = Class.create();
NXThemes.UnifiedStorage.prototype = Object.extend(
  new NXThemes.StorageAdapter(), {

  setup: function() {
    var models = [];
    var storage = this;

    // merge the data from all storages
    NXThemes.registerEventHandler('stored', storage, function(event) {
      event.subscriber.merge(event.publisher.read());
      // propagate the event
      NXThemes.notify('stored', {publisher: storage});
    });

    this.model.def.storage.units.each(function(p) {
      var model = NXThemes.getModelById(p);
      models.push(model);
      NXThemes.subscribe('stored',
        {subscriber: storage, publisher: model.storage}
      );
    });
    this.models = models;
  },

  requestData: function() {
    var model = this.model;
    this.models.each(function(m) {
      m.storage.requestData();
    });
  },

  storeData: function(data) {
    var model = this.model;
    this.models.each(function(m) {
      m.storage.storeData(data);
    });
  }
});

