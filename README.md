This project started as a personal POC by Michael Vorburger.ch to
evaluate the feasibility of using etcd as data store for OpenDaylight (ODL),
instead of its current "home made" CDS based on Akka.

The plan is contribute this to opendaylight.org, if and when successful.


## How to build?

If you're hitting an NPE in org.apache.maven.plugins:maven-javadoc-plugin:3.0.0:jar,
just use `mvn -Dmaven.javadoc.skip=true clean install` to work around it; it's possibly
related to not having JAVA_HOME environment variable (what's weird is that this NPE
does not seem to happen with other OpenDaylight builds).

To get a more recent version of jetcd-core than the currently used 0.0.2, just
`git clone https://github.com/coreos/jetcd.git ; cd jetcd; mvn [-DskipTests] clean install`
and change the dependency to jetcd-core in jetcd/pom.xml from 0.0.2 to 0.1.0-SNAPSHOT.


## *WIP* Architecture Design *WIP*

The `EtcdDataStore` implements `DOMStore` and internally uses `DataTree`, just like the `sal-distributed-datastore` (CDS) internally does.

On `commit()`, the put/merge/delete writes from `DataTreeModification` / `DataTreeCandidate` are sent to etcd.
Each `DataTreeCandidateNode`s is stored as an individual sub key/value - without their respective child nodes.
The data is stored in a compact binary serialization format (not e.g. XML or JSON).

*Following are NOTES for DISCUSSIONS, what is described here is NOT yet implemented:*

On read, we have to remote read from etcd, to guarantee strong consistency.  (Unless one were to request an eventual consistency read with a TBD API; then it could read from the local DataTree.)  The ser/der and tree-reconstruction overhead for every read can perhaps be optimized, if the "TX counter" proves we have that "generation" local already.

Both Distributed Change Listeners and Validation needs up-to-date data; this needs to be factored into above.

etcd instances would typically best be localhost co-located with the ODL nodes.


## FAQ

* _What is the status of this project?_ As of late July 2018, it's Proof of Concept (POC) with the `EtcdDBTest` illustrating, successfully, that the ODL MD SAL DataBroker API can be implemented on top of the etcd data store.

* _How can you try this out?_ The "packaging" work to make this available as a Karaf feature, and (more importantly) some re-factorings required in ODL to make it easy to install instead of CDS for real world testing is still to be done.

* _How can you help?_ Please see the [TODO.md](TODO.md) and start contributing!
