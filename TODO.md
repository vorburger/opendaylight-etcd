
- [ ] clean up EtcdDataStore print() VS real write()

- [ ] grep usages of DataTreeCandidateInputOutput and SerializationUtils

- [ ] re-use some of org.opendaylight.controller.cluster.datastore.persisted.DataTreeCandidateInputOutput?
      Can't completely as-is, because we need DataTreeCandidateNode granularity, not DataTreeCandidate.

- [ ] study watcher API and use it to read initial state?

- [ ] NormalizedNodeStreamWriter VS NormalizedNodeDataOutput; use e.g. LoggingNormalizedNodeStreamWriter ?

- [ ] study jetcd Txn and make class Etcd put() etc. transactional

- [ ] implement Chopper with snip() & snap() operations

- [ ] instead EtcdDataStore extends InMemoryDOMDataStore, just copy/paste all of it?!
      "you should just need an InMemoryDataTree. Pattern after ShardDataTree instead."
      https://git.opendaylight.org/gerrit/#/c/73208/

- [ ] make clustering tests, which start several etcd; write to one, read from another

- [ ] Transactions now done or more needed?!

- [ ] jetcd (?)


