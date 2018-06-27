
- [ ] study watcher API and use it to read initial state?

- [ ] study jetcd Txn and make class Etcd put() etc. transactional

- [ ] NormalizedNodeStreamWriter VS NormalizedNodeDataOutput; use e.g. LoggingNormalizedNodeStreamWriter ?

- [ ] implement Chopper with snip() & snap() operations (no longer needed, now?)

- [ ] instead EtcdDataStore extends InMemoryDOMDataStore, just copy/paste all of it?!
      "you should just need an InMemoryDataTree. Pattern after ShardDataTree instead."
      https://git.opendaylight.org/gerrit/#/c/73208/

- [ ] make clustering tests, which start several etcd; write to one, read from another

- [ ] Transactions now done or more needed?!

- [ ] MUCH clean-up and MANY TODOs ;)

- [ ] Karaf feature, using https://github.com/coreos/jetcd/pull/269 - or only support opendaylight-simple? :)
