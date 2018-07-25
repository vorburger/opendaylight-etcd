- [ ] test delete root

- [ ] Reduce what we are saving way too much - keys are double (as also inside NormalizedNode), and container node also contains leaf node

- [ ] git filter out the (un-used) vorburger.etcd sub-project into a separate repo

- [ ] TransformerImpl could chain to / stream directly from a TreeBuilder

- [ ] figure out remaining encode/decode issue(s) by, much, extending test model

- [ ] make EtcdWatcher read initial DataTree state... does it need a revision?

- [ ] programmatic "rm -rf testutils/target/etcd" via dataDir, like in MariaDB4j

- [ ] figure out why EtcdWatcher never prints watch hits.. run as a separate main() process, any different?

- [ ] study jetcd Txn and make class Etcd put() etc. transactional

- [ ] NormalizedNodeStreamWriter VS NormalizedNodeDataOutput; use e.g. LoggingNormalizedNodeStreamWriter ?

- [ ] implement Chopper with snip() & snap() operations (no longer needed, now?)

- [ ]refactor code to make generic non-etcd specific kv layer, pluggable for other KV stores

- [ ] instead EtcdDataStore extends InMemoryDOMDataStore, just copy/paste all of it?!
      "you should just need an InMemoryDataTree. Pattern after ShardDataTree instead."
      https://git.opendaylight.org/gerrit/#/c/73208/

- [ ] make clustering tests, which start several etcd; write to one, read from another

- [ ] Transactions now done or more needed?!

- [ ] MUCH clean-up and MANY TODOs ;)

- [ ] Karaf feature, using https://github.com/coreos/jetcd/pull/269 - or only support opendaylight-simple? :)

- [ ] compare performance of this VS CDS? But *DO* realize that real app performance issues are NOT because of slow datastore anyway..

- [ ] properly performance profile the code
