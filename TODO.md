
- [X] make EtcdWatcher continuously read DataTree state... does it need a revision?
- [ ] watch must filter out our own operations
- [ ] remove initialLoad() ?

- [ ] study jetcd Txn and make class Etcd put() etc. transactional
- [ ] Watcher's updates must be applied properly transactionally instead of each individually

- [ ] update README to document architecture better
- [ ] publicize etc.
- [ ] ODL repo and carry on there

- [ ] EntityOwnershipService EOS ?

- [ ] make etcd clustering tests (start several EtcdLauncher, not just clients)

- [ ] git filter out the (un-used) dom2kv/ sub-project into a separate repo

- [ ] refactor code to make generic non-etcd specific kv layer, pluggable for other KV stores
- [ ] Find that site doing perf. compares of data stores again.. is is worth adapting to:  Redis?  Couch DB?  Infinispan?

- [ ] instead EtcdDataStore extends InMemoryDOMDataStore, discuss an upstream artifact for what is shared
      "you should just need an InMemoryDataTree. Pattern after ShardDataTree instead."
      https://git.opendaylight.org/gerrit/#/c/73208/

- [ ] MUCH clean-up and MANY TODOs ;)

- [ ] get rid of jetcd/ artifact (as jetcd already ships an OSGi bundle and Karaf feature, now; just not released...)

- [ ] EtcdDOMDataBrokerWiring needs to refactor and move from testutils/ into ds/ to become runtime *Wiring

- [ ] build a JUnitRule for EtcdLauncher, like https://github.com/vorburger/MariaDB4j/pull/139 did for MariaDB4j

- [ ] add infrautils.metrics Meters & Timers to implementation

- [ ] com.coreos.jetcd.Maintenance ?

- [ ] Karaf feature, using https://github.com/coreos/jetcd/pull/269 - or only support opendaylight-simple? :)

- [ ] safe keys in a much more compact form; basically do compression, by keeping a dictionary (persisted in etcd) of all PathArgument

- [ ] compare performance of this VS CDS? But *DO* realize that real app performance issues are NOT because of slow datastore anyway..

- [ ] properly performance profile the code
