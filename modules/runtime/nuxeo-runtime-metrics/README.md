# About nuxeo-runtime-metrics


Since Nuxeo 5.7 Coda Hale Yammer Metrics is used to add metrics to Nuxeo.
This module is here to configure the metrics reporter and add
default metrics.

For testing you can run a graphite/grafana stack using the provided [docker compose](./docker/graphite-grafana):
```bash
cd ./docker/graphite-grafana
docker-compose up -d
```

Then configure your `nuxeo.conf` to report metrics and restart Nuxeo:
```bash
metrics.graphite.enabled=true
metrics.graphite.host=localhost
metrics.graphite.port=2003
metrics.graphite.period=30
metrics.tomcat.enabled=true
metrics.log4j.enabled=true
```

Access Grafana using [http://localhost:3000](http://localhost:3000) with user: admin pwd: admin,
you should have a Nuxeo Grafana dashboard up and running.

Graphite is also reachable [http://localhost:8000](http://localhost:8000)

To stop Grafana (loosing all data):
```bash
docker-compose down
```

For production you should use different retentions and docker volumes to persist data.


You can also find old example of json graphite dashboard [graphite](./graphite) directory.
To use it from Graphite Dashboard > Edit Dashboard, paste the content


See <http://doc.nuxeo.org/> for full documentation.
See <http://metrics.codahale.com/> for Metrics documentation.
