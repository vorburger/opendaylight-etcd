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
`git clone https://github.com/coreos/jetcd.git ; cd jetcd ; mvn [-DskipTests] clean install`
and change the `jetcd-core` dependency in jetcd/pom.xml from 0.0.2 to 0.1.0-SNAPSHOT.


## Architecture Design

A cluster of Etcd servers appears as a single logical server to us.
Details about its internal clustering, Raft implementation etc. are transparent to us.

The `EtcdDataStore` implements `DOMStore` and internally uses `DataTree`, just like the `sal-distributed-datastore` (CDS) does.

On `commit()`, the put/merge/delete writes from `DataTreeModification` / `DataTreeCandidate` are sent to etcd.
Each `DataTreeCandidateNode`s is stored as an individual sub key/value - without their respective child nodes.
The data is stored in a compact binary serialization format (not e.g. XML or JSON).
Changes from `DataTreeCandidate` are sent atomically to etcd (using `TXN`, not `PUT`).

We watch etcd, and update our internal `DataTree` as and when we receive change events.
Changes from watch events to the `DataTree` are applied atomically.

To guarantee strong consistency, we (remote) check the current revision on etcd, for a every new transaction,
and await having received and processed watch events at least up to that current revision.  This is what blocks reads.

If `DataBroker` offered an eventual consistency read API to applications, then it would be trivial to
offer (optionally) blazing fast reads (directly from the local `DataTree`, without any remoting.

We never do any `GET` on etcd to read data, but always serve directly from the `DataTree`.
There is no ser/der and tree-reconstruction overhead for reads (but there is when processing watch events).

This approach also guarantees that we have up-to-date data for validation.

Distributed Data Change Listeners also work as expected with this mechanism.


## Deployment Considerations

etcd instances would typically best be localhost co-located with the ODL nodes.


## FAQ

* _What is the status of this project?_ As of late July 2018, it's Proof of Concept (POC) with the `EtcdDBTest` illustrating, successfully, that the ODL MD SAL DataBroker API can be implemented on top of the etcd data store.

* _What's the point of this?_ The main goal is to have the option in ODL to completely avoid the home grown Raft/clustering code.  This will ease maintenance.  Increasing ODL performance (compared to CDS) is not a goal of this project.

* _How can you try this out?_ The "packaging" work to make this available as a Karaf feature, and (more importantly) some re-factorings required in ODL to make it easy to install instead of CDS for real world testing is still to be done.

* _How can you help?_ Please see the [TODO.md](TODO.md) and start contributing!
