var nuxeojs = (function(nuxeojs) {

  if (typeof(log) === 'undefined') {
    log = function() {};
  }

  function join() {
    var args = Array.prototype.slice.call(arguments);
    for (var i = args.length - 1; i >= 0; i--) {
      if (args[i] === null || args[i] === undefined || (typeof args[i] == 'string' && args[i].length === 0)) {
        args.splice(i, 1);
      }
    }
    var joined = args.join('/');
    return joined.replace(/(^\/+)|([^:])\/\/+/g, '$2/');
  }

  var DEFAULT_CLIENT_OPTIONS = {
    baseURL: '/nuxeo',
    restPath: 'site/api/v1',
    automationPath: 'site/api/v1/automation',
    auth: {
      method: 'basic',
      username: null,
      password: null
    },
    timeout: 30000
  };

  var Client = function(options) {
    options = jQuery.extend(true, {}, DEFAULT_CLIENT_OPTIONS, options || {});
    this._baseURL = options.baseURL;
    this._restURL = join(this._baseURL, options.restPath);
    this._automationURL = join(this._baseURL, options.automationPath);
    this._auth = options.auth;
    this._repositoryName = options.repositoryName || 'default';
    this._schemas = options.schemas || [];
    this._headers = options.headers || {};
    this._timeout = options.timeout;
    this.connected = false;

    this._xhrFields = {};
    this._initAuthentication();
  };

  Client.prototype._initAuthentication = function() {
    switch (this._auth.method) {
      case 'basic':
        if (this._auth.username && this._auth.password) {
          this._headers['Authorization'] = 'Basic ' + btoa(this._auth.username + ':' + this._auth.password);
          this._xhrFields = {
            withCredentials: true
          }
        }
        break;
    }
  };

  Client.prototype.connect = function(callback) {
    var self = this;
    var headers = jQuery.extend(true, {}, this._headers);
    headers['Accept'] = 'application/json';

    jQuery.ajax({
      type: 'POST',
      url: join(this._automationURL, 'login'),
      headers: headers,
      xhrFields: this._xhrFields
    })
      .done(function(data, textStatus, jqXHR) {
        if (data['entity-type'] === 'login'
          && (!self._auth.username || data['username'] === self._auth.username)) {
          self.connected = true;
          if (callback) {
            callback(null, self)
          }
        } else {
          if (callback) {
            callback(data, self)
          }
        }
      })
      .fail(function(jqXHR, textStatus, errorThrown) {
        if (callback) {
          callback(errorThrown, self)
        }
      });
  };
  Client.prototype.header = function(name, value) {
    this._headers[name] = value;
    return this;
  };

  Client.prototype.headers = function(headers) {
    this._headers = jQuery.extend(true, {}, this._headers, headers);
    return this;
  };

  Client.prototype.timeout = function(timeout) {
    this._timeout = timeout;
    return this;
  };

  Client.prototype.repositoryName = function(repositoryName) {
    this._repositoryName = repositoryName;
    return this;
  };

  Client.prototype.schema = function(schema) {
    this._schemas.push(schema);
    return this;
  };

  Client.prototype.schemas = function(schemas) {
    this._schemas.push.apply(this._schemas, schemas);
    return this;
  };

  Client.prototype.fetchOperationDefinitions = function(callback) {
    var headers = {
      'Accept': 'application/json'
    };

    jQuery.ajax({
      type: 'GET',
      url: this._automationURL,
      headers: headers,
      xhrFields: this._xhrFields
    })
      .done(function(data, textStatus, jqXHR) {
        if (callback) {
          callback(null, data, jqXHR);
        }
      })
      .fail(function(jqXHR, textStatus, errorThrown) {
        if (callback) {
          callback(errorThrown, null, jqXHR);
        }
      });
  };

  Client.prototype.operation = function(id) {
    return new Operation({
      id: id,
      url: this._automationURL,
      timeout: this._timeout,
      repositoryName: this._repositoryName,
      schemas: this._schemas,
      headers: this._headers
    });
  };

  Client.prototype.request = function(path) {
    return new Request({
      url: this._restURL,
      path: path,
      timeout: this._timeout,
      repositoryName: this._repositoryName,
      schemas: this._schemas,
      headers: this._headers
    });
  };

  Client.prototype.document = function(data) {
    return new Document({
      client: this,
      data: data,
      timeout: this._timeout,
      repositoryName: this._repositoryName,
      schemas: this._schemas,
      headers: this._headers
    });
  };

  Client.prototype.uploader = function(options) {
    options = jQuery.extend(true, options, {
      automationURL: this._automationURL,
      timeout: this._timeout,
      repositoryName: this._repositoryName,
      headers: this._headers
    })
    return new Uploader(options);
  };

  nuxeojs.Client = Client;


  var Operation = function(options) {
    this._id = options.id;
    this._url = options.url;
    this._timeout = options.timeout;
    this._overrideMimeType = options.overrideMimeType;
    this._repositoryName = options.repositoryName;
    this._schemas = [].concat(options.schemas);
    this._headers = options.headers || {};
    this._automationParams = {
      params: {},
      context: {},
      input: undefined
    };

    this.header('X-NXVoidOperation', false);
  };

  Operation.prototype.timeout = function(timeout) {
    this._timeout = timeout;
    return this;
  };

  Operation.prototype.overrideMimeType = function(overrideMimeType) {
    this._overrideMimeType = overrideMimeType;
    return this;
  };

  Operation.prototype.header = function(name, value) {
    this._headers[name] = value;
    return this;
  };

  Operation.prototype.headers = function(headers) {
    this._headers = jQuery.extend(true, {}, this._headers, headers);
    return this;
  };

  Operation.prototype.repositoryName = function(repositoryName) {
    this._repositoryName = repositoryName;
    return this;
  };

  Operation.prototype.schema = function(schema) {
    this._schemas.push(schema);
    return this;
  };

  Operation.prototype.schemas = function(schemas) {
    this._schemas.push.apply(this._schemas, schemas);
    return this;
  };

  Operation.prototype.param = function(name, value) {
    this._automationParams.params[name] = value;
    return this;
  };

  Operation.prototype.params = function(params) {
    this._automationParams.params = jQuery.extend(true, {}, this._automationParams.params, params);
    return this;
  };

  Operation.prototype.context = function(context) {
    this._automationParams.context = context;
    return this;
  };

  Operation.prototype.input = function(input) {
    this._automationParams.input = input;
    return this;
  };

  Operation.prototype.voidOperation = function(voidOperation) {
    this.header('X-NXVoidOperation', voidOperation);
    return this;
  };

  Operation.prototype.execute = function(options, callback) {
    function getOperationURL(url, operationId) {
      if (url.indexOf('/', url.length - 1) == -1) {
        url += '/';
      }
      url += operationId;
      return url;
    }

    if (typeof options === 'function') {
      // no options
      callback = options;
      options = {};
    }
    options = options || {};

    var headers = jQuery.extend(true, {}, this._headers);
    headers['Nuxeo-Transaction-Timeout'] = 5 + (this._timeout / 1000) | 0;
    if (this._schemas.length > 0) {
      headers['X-NXDocumentProperties'] = this._schemas.join(',');
    }
    if (this._repositoryName !== undefined) {
      headers['X-NXRepository'] = this._repositoryName;
    }
    headers = jQuery.extend(true, headers, options.headers || {});

    var self = this;
    var xhrParams = {
      type: 'POST',
      timeout: this._timeout,
      headers: headers,
      url: getOperationURL(this._url, this._id),
      xhrFields: this._xhrFields,
      beforeSend: function(xhr) {
        if (self._overrideMimeType) {
          xhr.overrideMimeType(self._overrideMimeType);
        }
      }
    };

    if (typeof this._automationParams.input === 'object') {
      // multipart
      var automationParams = {
        params: this._automationParams.params,
        context: this._automationParams.context
      };

      var formData = new FormData();
      var params = new Blob([JSON.stringify(automationParams)], {
        'type': 'application/json+nxrequest'
      });
      formData.append('request', params, 'request');
      formData.append(options.filename, this._automationParams.input, options.filename);

      xhrParams.data = formData;
      xhrParams.processData = false;
      xhrParams.contentType = 'multipart/form-data';
    } else {
      xhrParams.data = JSON.stringify(this._automationParams);
      xhrParams.contentType = 'application/json+nxrequest';
    }

    jQuery.ajax(xhrParams)
      .done(function(data, textStatus, jqXHR) {
        if (callback) {
          callback(null, data, jqXHR)
        }
      })
      .fail(function(jqXHR, textStatus, errorThrown) {
        if (callback) {
          callback(errorThrown, null, jqXHR)
        }
      });
  };

  Operation.prototype.uploader = function(options) {
    options = jQuery.extend(true, {}, options, {
      operationId: this._id,
      url: this._url,
      timeout: this._timeout,
      repositoryName: this._repositoryName,
      headers: this._headers,
      automationParams: this._automationParams
    });
    if (!this._uploader) {
      this._uploader = new Uploader(options)
    }
    return this._uploader;
  };

  nuxeojs.Operation = Operation;


  var Request = function(options) {
    this._path = options.path || '';
    this._url = options.url;
    this._timeout = options.timeout;
    this._overrideMimeType = options.overrideMimeType;
    this._repositoryName = options.repositoryName;
    this._schemas = [].concat(options.schemas);
    this._headers = options.headers || {};
    this._query = options.query || {};
  };

  Request.prototype.timeout = function(timeout) {
    this._timeout = timeout;
    return this;
  };

  Request.prototype.overrideMimeType = function(overrideMimeType) {
    this._overrideMimeType = overrideMimeType;
    return this;
  };

  Request.prototype.header = function(name, value) {
    this._headers[name] = value;
    return this;
  };

  Request.prototype.headers = function(headers) {
    this._headers = jQuery.extend(true, {}, this._headers, headers);
    return this;
  };

  Request.prototype.repositoryName = function(repositoryName) {
    this._repositoryName = repositoryName;
    return this;
  };

  Request.prototype.schema = function(schema) {
    this._schemas.push(schema);
    return this;
  };

  Request.prototype.schemas = function(schemas) {
    this._schemas.push.apply(this._schemas, schemas);
    return this;
  };

  Request.prototype.query = function(query) {
    this._query = jQuery.extend(true, {}, this._query, query);
    return this;
  };

  Request.prototype.path = function(path) {
    this._path = join(this._path, path);
    return this;
  };

  Request.prototype.get = function(options, callback) {
    if (typeof options === 'function') {
      // no options
      callback = options;
      options = {};
    }
    options = jQuery.extend(true, {}, options, {
      method: 'get'
    });
    this.execute(options, callback);
  };

  Request.prototype.post = function(options, callback) {
    if (typeof options === 'function') {
      // no options
      callback = options;
      options = {};
    }
    this.headers({
      'Content-Type': 'application/json'
    });
    if (options.data && typeof options.data !== 'string') {
      options.data = JSON.stringify(options.data);
    }
    options = jQuery.extend(true, options, {
      method: 'post'
    });
    this.execute(options, callback);
  };

  Request.prototype.put = function(options, callback) {
    if (typeof options === 'function') {
      // no options
      callback = options;
      options = {};
    }
    this.headers({
      'Content-Type': 'application/json'
    });
    if (options.data && typeof options.data !== 'string') {
      options.data = JSON.stringify(options.data);
    }
    options = jQuery.extend(true, options, {
      method: 'put'
    });
    this.execute(options, callback);
  };

  Request.prototype.delete = function(options, callback) {
    if (typeof options === 'function') {
      // no options
      callback = options;
      options = {};
    }
    this.headers({
      'Content-Type': 'application/json'
    });
    options = jQuery.extend(true, options, {
      method: 'delete'
    });
    this.execute(options, callback);
  };

  Request.prototype.execute = function(options, callback) {
    if (typeof options === 'function') {
      // no options
      callback = options;
      options = {};
    }
    options = options || {};
    options.method = options.method || 'get';

    var headers = jQuery.extend(true, {}, this._headers);
    headers['Accept'] = 'application/json';
    headers['Nuxeo-Transaction-Timeout'] = 5 + (this._timeout / 1000) | 0;
    if (this._schemas.length > 0) {
      headers['X-NXDocumentProperties'] = this._schemas.join(',');
    }
    headers = jQuery.extend(true, headers, options.headers || {});

    // stringify if needed
    if (headers['Content-Type'] === 'application/json') {
      if (options.data && typeof options.data === 'object') {
        options.data = JSON.stringify(options.data);
      }
    }

    // query params
    var query = jQuery.extend(true, {}, this._query);
    query = jQuery.extend(true, query, options.query || {});

    var path = '';
    if (this._repositoryName !== undefined) {
      path = join('repo', this._repositoryName);
    }
    path = join(path, this._path);
    var data = options.data || query;

    var xhrParams = {
      type: options.method,
      timeout: this._timeout,
      headers: headers,
      data: data,
      url: join(this._url, path),
      xhrFields: this._xhrFields,
      beforeSend: function(xhr) {
        if (self._overrideMimeType) {
          xhr.overrideMimeType(self._overrideMimeType);
        }
      }
    };

    jQuery.ajax(xhrParams)
      .done(function(data, textStatus, jqXHR) {
        if (callback) {
          callback(null, data, jqXHR)
        }
      })
      .fail(function(jqXHR, textStatus, errorThrown) {
        if (callback) {
          callback(errorThrown, null, jqXHR)
        }
      });
  };

  nuxeojs.Request = Request;

  var Document = function(options) {
    this._client = options.client;
    this._timeout = options.timeout;
    this._repositoryName = options.repositoryName;
    this._schemas = [].concat(options.schemas);
    this._headers = options.headers || {};
    this.properties = {};
    this.dirtyProperties = {};

    var data = options.data;
    if (typeof data === 'string') {
      // id or path ref
      if (data.indexOf('/') === 0) {
        this.path = data;
      } else {
        this.uid = data;
      }
    } else if (typeof data === 'object') {
      // JSON doc
      jQuery.extend(true, this, data);
    } else {
      // unsupported
      throw new Error();
    }
  };

  Document.prototype.timeout = function(timeout) {
    this._timeout = timeout;
    return this;
  };

  Document.prototype.header = function(name, value) {
    this._headers[name] = value;
    return this;
  };

  Document.prototype.headers = function(headers) {
    this._headers = jQuery.extend(true, {}, this._headers, headers);
    return this;
  };

  Document.prototype.repositoryName = function(repositoryName) {
    this.repository = repositoryName;
    return this;
  };

  Document.prototype.schema = function(schema) {
    this._schemas.push(schema);
    return this;
  };

  Document.prototype.schemas = function(schemas) {
    this._schemas.push.apply(this._schemas, schemas);
    return this;
  };

  Document.prototype.adapter = function(adapter) {
    this._adapter = adapter;
    return this;
  };

  Document.prototype.set = function(properties) {
    this.dirtyProperties = jQuery.extend(true, {}, this.dirtyProperties, properties);
    this.properties = jQuery.extend(true, {}, this.properties, properties);
    return this;
  };

  Document.prototype.fetch = function(callback) {
    var self = this;
    var path = this.uid !== undefined ? join('id', this.uid) : join('path', this.path);
    if (this._adapter !== undefined) {
      path = join(path, '@bo', this._adapter);
    }
    var request = this._client.request(path);
    request.timeout(this._timeout).schemas(this._schemas).headers(this._headers)
      .repositoryName(this.repository);
    request.get(function(error, data, response) {
      if (data !== undefined && typeof data === 'object' && data['entity-type'] === 'document') {
        data = self._client.document(data);
      }
      if (callback) {
        callback(error, data, response);
      }
    });
  };

  Document.prototype.create = function(data, callback) {
    var self = this;
    var path = this.uid !== undefined ? join('id', this.uid) : join('path', this.path);
    if (this._adapter !== undefined) {
      path = join(path, '@bo', this._adapter, data.name);
    }
    var request = this._client.request(path);
    request.timeout(this._timeout).schemas(this._schemas).headers(this._headers)
      .repositoryName(this.repository);
    request.post({
      data: data
    }, function(error, data, response) {
      if (data !== undefined && typeof data === 'object' && data['entity-type'] === 'document') {
        data = self._client.document(data);
      }
      if (callback) {
        callback(error, data, response);
      }
    });
  };

  Document.prototype.copy = function(data, callback) {
    var self = this;
    var operation = this._client.operation('Document.Copy');
    operation.timeout(this._timeout).schemas(this._schemas).headers(this._headers)
      .repositoryName(this.repository)
      .input(this.uid).params(data);
    operation.execute(function(error, data, response) {
      if (data !== undefined && typeof data === 'object' && data['entity-type'] === 'document') {
        data = self._client.document(data);
      }
      if (callback) {
        callback(error, data, response);
      }
    });
  }

  Document.prototype.move = function(data, callback) {
    var self = this;
    var operation = this._client.operation('Document.Move');
    operation.timeout(this._timeout).schemas(this._schemas).headers(this._headers)
      .repositoryName(this.repository)
      .input(this.uid).params(data);
    operation.execute(function(error, data, response) {
      if (data !== undefined && typeof data === 'object' && data['entity-type'] === 'document') {
        data = self._client.document(data);
      }
      if (callback) {
        callback(error, data, response);
      }
    });
  }

  Document.prototype.update = function(data, callback) {
    var self = this;
    var path = this.uid !== undefined ? join('id', this.uid) : join('path', this.path);
    if (this._adapter !== undefined) {
      path = join(path, '@bo', this._adapter);
    }
    var request = this._client.request(path);
    request.timeout(this._timeout).schemas(this._schemas).headers(this._headers)
      .repositoryName(this.repository);
    request.put({
      data: data
    }, function(error, data, response) {
      if (data !== undefined && typeof data === 'object' && data['entity-type'] === 'document') {
        data = self._client.document(data);
      }
      if (callback) {
        callback(error, data, response);
      }
    });
  };

  Document.prototype.delete = function(callback) {
    var self = this;
    var path = this.uid !== undefined ? join('id', this.uid) : join('path', this.path);
    var request = this._client.request(path);
    request.timeout(this._timeout).schemas(this._schemas).headers(this._headers).repositoryName(this.repository);
    request.delete(function(error, data, response) {
      if (data !== undefined && typeof data === 'object' && data['entity-type'] === 'document') {
        data = self._client.document(data);
      }
      if (callback) {
        callback(error, data, response);
      }
    });
  };

  Document.prototype.save = function(callback) {
    this.update({
      uid: this.uid,
      properties: this.dirtyProperties
    }, callback);
  };

  Document.prototype.children = function(callback) {
    var self = this;
    var path = this.uid !== undefined ? join('id', this.uid) : join('path', this.path);
    path = join(path, '@children');
    var request = this._client.request(path);
    request.timeout(this._timeout).schemas(this._schemas).headers(this._headers)
      .repositoryName(this.repository);
    request.get(function(error, data, response) {
      if (data !== undefined && typeof data === 'object' && data['entity-type'] === 'document') {
        data = self._client.document(data);
      }
      if (callback) {
        callback(error, data, response);
      }
    });
  };

  Document.prototype.isFolder = function() {
    return this.facets.indexOf('Folderish') !== -1;
  };

  nuxeojs.Document = Document;


  var DEFAULT_UPLOADER_OPTIONS = {
    numConcurrentUploads: 5,
    // define if upload should be triggered directly
    directUpload: true,
    // update upload speed every second
    uploadRateRefreshTime: 1000,
    batchStartedCallback: function(batchId) {},
    batchFinishedCallback: function(batchId) {},
    uploadStartedCallback: function(fileIndex, file) {},
    uploadFinishedCallback: function(fileIndex, file, time) {},
    uploadProgressUpdatedCallback: function(fileIndex, file, newProgress) {},
    uploadSpeedUpdatedCallback: function(fileIndex, file, speed) {}
  };


  var Uploader = function(options) {
    options = jQuery.extend(true, {}, DEFAULT_UPLOADER_OPTIONS, options || {});
    this._url = options.url;
    this._operationId = options.operationId;
    this._automationParams = {
      params: {},
      context: {},
      input: undefined
    };
    this._automationParams = jQuery.extend(true, this._automationParams, options.automationParams || {});
    this._numConcurrentUploads = options.numConcurrentUploads;
    this._directUpload = options.directUpload;
    this._uploadRateRefreshTime = options.uploadRateRefreshTime;
    this._batchStartedCallback = options.batchStartedCallback;
    this._batchFinishedCallback = options.batchFinishedCallback;
    this._uploadStartedCallback = options.uploadStartedCallback;
    this._uploadFinishedCallback = options.uploadFinishedCallback;
    this._uploadProgressUpdatedCallback = options.uploadProgressUpdatedCallback;
    this._uploadSpeedUpdatedCallback = options.uploadSpeedUpdatedCallback;
    this._timeout = options.timeout;
    this._repositoryName = options.repositoryName;
    this._headers = options.headers || {};
    this._sendingRequestsInProgress = false;
    this._uploadStack = [];
    this._uploadIndex = 0;
    this._nbUploadInProgress = 0;
    this._completedUploads = [];

    this.batchId = 'batch-' + new Date().getTime() + '-' + Math.floor(Math.random() * 100000);
    this._automationParams.params['operationId'] = this._operationId;
    this._automationParams.params['batchId'] = this.batchId;
  };

  Uploader.prototype.timeout = function(timeout) {
    this._timeout = timeout;
    return this;
  };

  Uploader.prototype.header = function(name, value) {
    this._headers[name] = value;
    return this;
  };

  Uploader.prototype.headers = function(headers) {
    this._headers = jQuery.extend(true, {}, this._headers, headers);
    return this;
  };

  Uploader.prototype.repositoryName = function(repositoryName) {
    this._repositoryName = repositoryName;
    return this;
  };

  Uploader.prototype.uploadFile = function(file, callback) {
    if (callback) {
      file.callback = callback;
    }
    this._uploadStack.push(file);
    if (this._directUpload && !this._sendingRequestsInProgress) {
      this.uploadFiles();
    }
  };

  Uploader.prototype.uploadFiles = function() {
    var self = this;
    if (this._nbUploadInProgress >= this._numConcurrentUploads) {
      this._sendingRequestsInProgress = false;
      log('delaying upload for next file(s) ' + this._uploadIndex + '+ since there are already ' + this._nbUploadInProgress + ' active uploads');
      return;
    }

    this._batchStartedCallback(this.batchId);

    this._sendingRequestsInProgress = true;
    while (this._uploadStack.length > 0) {
      var file = this._uploadStack.shift();
      // create a new xhr object
      var xhr = new XMLHttpRequest();
      var upload = xhr.upload;
      upload.fileIndex = this._uploadIndex + 0;
      upload.fileObj = file;
      upload.downloadStartTime = new Date().getTime();
      upload.currentStart = upload.downloadStartTime;
      upload.currentProgress = 0;
      upload.startData = 0;
      upload.batchId = this.batchId;

      // add listeners
      upload.addEventListener('progress', function(event) {
        self._progress(event)
      }, false);

      if (file.callback) {
        upload.callback = file.callback;
      }

      // The 'load' event doesn't work correctly on WebKit (Chrome,
      // Safari),
      // it fires too early, before the server has returned its response.
      // still it is required for Firefox
      var self = this;
      if (navigator.userAgent.indexOf('Firefox') > -1) {
        upload.addEventListener('load', function(event) {
          log('trigger load');
          log(event);
          self._load(event.target)
        }, false);
      }

      // on ready state change is not fired in all cases on webkit
      // - on webkit we rely on progress lister to detected upload end
      // - but on Firefox the event we need it
      xhr.onreadystatechange = (function(xhr) {
        return function() {
          self._readyStateChange(xhr)
        }
      })(xhr);

      // compute timeout in seconds and integer
      var uploadTimeoutS = 5 + (this._timeout / 1000) | 0;
      var targetUrl = join(this._url, 'batch/upload');

      var headers = jQuery.extend(true, {}, this._headers);
      headers['Cache-Control'] = 'no-cache';
      headers['X-Requested-With'] = 'XMLHttpRequest';
      headers['X-File-Name'] = encodeURIComponent(file.name);
      headers['X-File-Size'] = file.size;
      headers['X-File-Type'] = file.type;
      headers['X-Batch-Id'] = this.batchId;
      headers['X-File-Idx'] = this._uploadIndex;
      headers['Nuxeo-Transaction-Timeout'] = uploadTimeoutS;
      if (this._repositoryName !== undefined) {
        headers['X-NXRepository'] = this._repositoryName;
      }

      log('starting upload for file ' + this._uploadIndex);
      xhr.open('POST', targetUrl);
      jQuery.each(headers, function(key, value) {
        xhr.setRequestHeader(key, value);
      });

      this._nbUploadInProgress++;
      this._uploadStartedCallback(this._uploadIndex, file);
      this._uploadIndex++;

      xhr.send(file);

      if (this._nbUploadInProgress >= this._numConcurrentUploads) {
        this._sendingRequestsInProgress = false;
        log('delaying upload for next file(s) ' + this._uploadIndex + '+ since there are already ' + this._nbUploadInProgress + ' active uploads');
        return;
      }
    }
    this._sendingRequestsInProgress = false;
  };

  Uploader.prototype.execute = function(options, callback) {
    if (typeof options === 'function') {
      // no options
      callback = options;
      options = {};
    }
    options = options || {};

    var headers = jQuery.extend(true, {}, this._headers);
    headers['Nuxeo-Transaction-Timeout'] = 5 + (this._timeout / 1000) | 0;
    headers['Content-Type'] = 'application/json+nxrequest';
    if (this._repositoryName !== undefined) {
      headers['X-NXRepository'] = this._repositoryName;
    }
    headers = jQuery.extend(true, headers, options.headers || {});

    var xhrParams = {
      type: 'POST',
      timeout: this._timeout,
      headers: headers,
      data: JSON.stringify(this._automationParams),
      url: join(this._url, 'batch/execute'),
      xhrFields: this._xhrFields
    };

    jQuery.ajax(xhrParams)
      .done(function(data, textStatus, jqXHR) {
        if (callback) {
          callback(null, data, jqXHR)
        }
      })
      .fail(function(jqXHR, textStatus, errorThrown) {
        if (callback) {
          callback(errorThrown, null, jqXHR)
        }
      });
  };

  Uploader.prototype._readyStateChange = function(xhr) {
    var upload = xhr.upload;
    log('readyStateChange event on file upload ' + upload.fileIndex + ' (state : ' + xhr.readyState + ')');
    if (xhr.readyState == 4) {
      if (xhr.status == 200) {
        this._load(upload);
      } else {
        log('Upload failed, status: ' + xhr.status);
      }
    }
  };

  Uploader.prototype._load = function(upload) {
    var fileIdx = upload.fileIndex;
    log('Received loaded event on  file ' + fileIdx);
    if (this._completedUploads.indexOf(fileIdx) < 0) {
      this._completedUploads.push(fileIdx);
    } else {
      log('Event already processsed for file ' + fileIdx + ', exiting');
      return;
    }
    var now = new Date().getTime();
    var timeDiff = now - upload.downloadStartTime;
    this._uploadFinishedCallback(upload.fileIndex, upload.fileObj,
      timeDiff);
    log('upload of file ' + upload.fileIndex + ' completed');
    if (upload.callback) {
      upload.callback(upload.fileIndex, upload.fileObj,
        timeDiff);
    }
    this._nbUploadInProgress--;
    if (!this._sendingRequestsInProgress && this._uploadStack.length > 0 && this._nbUploadInProgress < this._numConcurrentUploads) {
      // restart upload
      log('restart pending uploads');
      this.uploadFiles();
    } else if (this._nbUploadInProgress == 0) {
      this._batchFinishedCallback(this.batchId);
    }
  };

  Uploader.prototype._progress = function(event) {
    log(event);
    if (event.lengthComputable) {
      var percentage = Math.round((event.loaded * 100) / event.total);
      if (event.target.currentProgress != percentage) {

        log('progress event on upload of file ' + event.target.fileIndex + ' --> ' + percentage + '%');

        event.target.currentProgress = percentage;
        this._uploadProgressUpdatedCallback(
          event.target.fileIndex, event.target.fileObj,
          event.target.currentProgress);

        var elapsed = new Date().getTime();
        var diffTime = elapsed - event.target.currentStart;
        if (diffTime >= this._uploadRateRefreshTime) {
          var diffData = event.loaded - event.target.startData;
          var speed = diffData / diffTime; // in KB/sec

          this._uploadSpeedUpdatedCallback(event.target.fileIndex,
            event.target.fileObj, speed);

          event.target.startData = event.loaded;
          event.target.currentStart = elapsed;
        }
        if (event.loaded == event.total) {
          log('file ' + event.target.fileIndex + ' detected upload complete');
          // having all the bytes sent to the server does not mean the
          // server did actually receive everything
          // but since load event is not reliable on Webkit we need
          // this
          // window.setTimeout(function(){load(event.target, opts);},
          // 5000);
        } else {
          log('file ' + event.target.fileIndex + ' not completed :' + event.loaded + '/' + event.total);
        }
      }
    }
  };

  nuxeojs.Uploader = Uploader;

  return nuxeojs;

})(nuxeojs || {});
