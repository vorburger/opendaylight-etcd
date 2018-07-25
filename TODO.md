
- [ ] Reduce what we are saving way too much - keys are double (as also inside NormalizedNode), and container node also contains leaf node
- [ ] figure out remaining encode/decode issue(s) by, much, extending test model

- [ ] make clustering tests, which start several etcd; write to one, read from another
- [ ] figure out why EtcdWatcher never prints watch hits.. run as a separate main() process, any different?
- [ ] make EtcdWatcher continuously read DataTree state... does it need a revision?

- [ ] study jetcd Txn and make class Etcd put() etc. transactional

- [ ] update README to document architecture
- [ ] publicize etc.
- [ ] ODL repo and carry on there

- [ ] git filter out the (un-used) dom2kv/ sub-project into a separate repo

- [ ] refactor code to make generic non-etcd specific kv layer, pluggable for other KV stores

- [ ] instead EtcdDataStore extends InMemoryDOMDataStore, discuss an upstream artifact for what is shared
      "you should just need an InMemoryDataTree. Pattern after ShardDataTree instead."
      https://git.opendaylight.org/gerrit/#/c/73208/

- [ ] MUCH clean-up and MANY TODOs ;)

- [ ] get rid of jetcd/ artifact (as jetcd already ships an OSGi bundle and Karaf feature, now; just not released...)

- [ ] testutils' TestEtcdDataBrokersProvider & EtcdConcurrentDataBrokerTestCustomizer should become runtime *Wiring instead

- [ ] add infrautils.metrics Meters & Timers to implementation

- [ ] Karaf feature, using https://github.com/coreos/jetcd/pull/269 - or only support opendaylight-simple? :)

- [ ] safe keys in a much more compact form; basically do compression, by keeping a dictionary (persisted in etcd) of all PathArgument

- [ ] compare performance of this VS CDS? But *DO* realize that real app performance issues are NOT because of slow datastore anyway..

- [ ] properly performance profile the code
