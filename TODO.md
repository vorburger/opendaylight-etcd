
- [ ] study jetcd Txn and make class Etcd put() etc. transactional

- [ ] implement Chopper with snip() & snap() operations

- [ ] instead EtcdDataStore extends InMemoryDOMDataStore, just copy/paste all of it?!
      "you should just need an InMemoryDataTree. Pattern after ShardDataTree instead."
      https://git.opendaylight.org/gerrit/#/c/73208/

- [ ] make clustering tests, which start several etcd; write to one, read from another

- [ ] Transactions now done or more needed?!

- [ ] jetcd (?)


