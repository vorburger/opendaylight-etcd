This project started as a personal POC by Michael Vorburger.ch to
evaluate the feasibility of using etcd as data store for YANG data in OpenDaylight (ODL),
instead of its current "home made" CDS based on Akka.

See this [presentation given in 2018.09 at the ODL DDF during ONS Europe in Amsterdam](https://docs.google.com/presentation/d/160GrKDb9kR-M48FKale09A-VF2_HSu3po_s0M4UReyU) for some background.

The plan is contribute this to opendaylight.org, if and when successful.


## How to build? [![Build Status](https://travis-ci.org/vorburger/opendaylight-etcd.svg?branch=master)](https://travis-ci.org/vorburger/opendaylight-etcd)

1. To get a more recent version of jetcd-core than the currently used 0.0.2, just
`git clone https://github.com/coreos/jetcd.git ; cd jetcd ; mvn [-DskipTests] clean install`


2. If you're hitting an NPE in org.apache.maven.plugins:maven-javadoc-plugin:3.0.0:jar,
just use `mvn -Dmaven.javadoc.skip=true -Dduplicate-finder.skip -DskipTests -Dcheckstyle.skip clean install` to work around it; it's possibly
related to not having JAVA_HOME environment variable (what's weird is that this NPE
does not seem to happen with other OpenDaylight builds).



## Architecture Design

A cluster of Etcd servers appears as a single logical server to clients like this.
Details about its internal clustering, Raft implementation etc. are transparent.

The `EtcdDataStore` implements `DOMStore` and internally uses a YANG `DataTree`, just like the `sal-distributed-datastore` (CDS) does.

On `commit()`, the put/merge/delete writes from `DataTreeModification` / `DataTreeCandidate` are sent to etcd.
Each `DataTreeCandidateNode` is stored as an individual sub key/value - without their respective child nodes.
This allows for fine-grained future updates and deletes.
Changes from `DataTreeCandidate` are sent atomically to etcd (using `TXN`, not `PUT`).

The data is stored in a compact binary serialization format (not e.g. XML or JSON).
The communication from the etcd client in ODL to the etcd server/s is similarly compact binary, not text-based over HTTP.

We watch etcd, and update our internal `DataTree` as and when we receive change events.
Changes from watch events are applied atomically to the `DataTree`.

To guarantee strong consistency, we (remote) check the current revision on etcd, for a every new transaction,
and await having received and processed watch events at least up to that current revision.  This is what blocks reads.

If `DataBroker` offered an eventual consistency read API to applications, then it would be trivial to
offer (optionally) blazing fast reads (directly from the local `DataTree`), without any remoting.

We never do any `GET` on etcd to read data, but always serve directly from the `DataTree`.
There is no ser/der and tree-reconstruction overhead for reads (but there is when processing watch events).

This approach also guarantees that we have up-to-date data for validation.

Distributed Data Change Listeners also work as expected with this mechanism.


## Deployment Considerations

etcd instances would typically best be localhost co-located with the ODL nodes.


## Demos

Make sure you have at least 1 etcd server running:

    sudo dnf install etcd
    sudo systemctl start etcd
    systemctl status etcd
    ETCDCTL_API=3 etcdctl get --from-key ''

or even just start it directly, without systemd, in the foreground in another terminal tab:

    cd /tmp
    etcd

    tree /tmp/default.etcd/
    ETCDCTL_API=3 etcdctl get --from-key ''

_TODO Document how to best easily start test cluster of 3 etcd servers locally..._

If you used etcd before you may want to completely wipe it:

    ETCDCTL_API=3 etcdctl del "" --from-key=true

Beware that `ETCDCTL_API=3 etcdctl get --from-key ''` [outputs binary](https://github.com/etcd-io/etcd/issues/10102), so better use our own:

    java -jar demo/target/*.jar read http://localhost:2379


### RESTCONF

Here is how to run [the asciinema POC v0.2](https://asciinema.org/a/201859):

Configure the etcd connection URL (_TODO this is a hack which will change to be a real configuration file later_):

    mkdir karaf/target/assembly/../../jetcd-launcher-maven-plugin/
    echo http://localhost:2379 >karaf/target/assembly/../../jetcd-launcher-maven-plugin/endpoint

Now start ODL and make sure it's happy:

    ./karaf/target/assembly/bin/karaf
    opendaylight-user@root>feature:list -i
    opendaylight-user@root>diag

Note how there is no `karaf/target/assembly/etc/org.opendaylight.controller.cluster.datastore.cfg` now, thus "proving" that we don't run the existing ODL datastore anymore.  You can now use RESTCONF as usual - but it runs on etcd! [Open apidoc/explorer](http://localhost:8181/apidoc/explorer/index.html) (admin/admin), and watch the logs (`tail -f karaf/target/assembly/data/log/karaf.log`) to understand what happens when you e.g. do:

    http -a admin:admin GET http://localhost:8181/restconf/config/opendaylight-etcd-test:HelloWorldContainer
    echo '<HelloWorldContainer xmlns="urn:opendaylight:etcd:test"><name>hello, world</name></HelloWorldContainer>' >put.xml
    http -v -a admin:admin PUT :8181/restconf/config/opendaylight-etcd-test:HelloWorldContainer @put.xml
    http -a admin:admin GET :8181/restconf/config/opendaylight-etcd-test:HelloWorldContainer
    java -jar demo/target/*.jar read http://localhost:2379
    http -a admin:admin DELETE :8181/restconf/config/opendaylight-etcd-test:HelloWorldContainer
    http -a admin:admin GET :8181/restconf/config/opendaylight-etcd-test:HelloWorldContainer


### Standalone

Here is how to run [the asciinema POC v0.1](https://asciinema.org/a/DShFpWOXFmaQV3AD5n8nHeHX6):

Now run this project's demo:

    java -jar demo/target/*.jar write http://localhost:4001

or if you started `etcd` directly without systemd then:

    java -jar demo/target/*.jar write http://localhost:2379

and have a closer look at the logs this will print to understand what happened, and read it via:

    java -jar demo/target/*.jar read http://localhost:2379


## FAQ

### About this project

* _What is the status of this project?_ As of late July 2018, it's a Proof of Concept (POC) with the `EtcdDBTest` illustrating, successfully, that the ODL MD SAL DataBroker API can be implemented on top of the etcd data store.

* _What's the point of this?_ The main goal is to have the option in ODL to completely avoid the home grown Raft/clustering code, and completely avoid Akka.  This will ease maintenance.

* _Is this project going to automagically solve all sorts of performance issues you may face in ODL today?_ Nope. Increasing ODL performance (compared to CDS) is not a goal of this project.

* _How can I access the data in etcd?_ Through ODL APIs (or RESTCONF, etc.) via the code in this project - as always.  It is an explicit non-goal of this project to allow "direct" access to the YANG data in etcd.  It is stored in an internal binary format, which may change.  It requires the YANG model schema to really make sense.  Don't read it directly.  What you could do however is run a [lightweight standalone "ODL"](https://github.com/vorburger/opendaylight-simple) process which uses this project.

* _How can you try this out?_ Much work still needs to be done! ;-) This e.g. includes, roughly in order: much more unit and integration tests (notably around concurrency), some re-factorings required in ODL to remove code copy/paste here during the POC, work to make it easy to install instead of the current implementation, packaging work to make this available as a Karaf feature, then much real world testing through CSITs, etc.

* _How can you help?_ Please see the [TODO.md](TODO.md) and start contributing!


### About some typical objections

* _But how will we upgrade the code from today's clustering solution to an etcd based datastore?_ The idea is that ultimately this will simply be a new alternative feature installation, and require absolutely no change to any existing application code.

* _But how will we migrate the data from today to tomorrow during customer upgrades?_ Replay based upgrades start with a fresh new empty datastore, so this is a non-issue.  (A non replay based upgrade procedures would have to export the datastore content using DAEXIM, and re-import a dump into an instance with an etcd datastore.)

* _But how can we "shard" with this?_ Supporting several "shards" and/or multiple etcd stores (for sharding, not clustering) is an explicit non-goal of v1 of this project.

* _But etcd doesn't seem to have a pure in-memory mode, so what about operational vs config?_  So in ODL the operational data store, contrary to the configuration, does not have to survive "restarts".  But perhaps it's OK if it does anyway.  If not, it would certainly be easily possible to explicitly wipe the content of the operational data store sub tree in etcd on the start of the ODL cluster (not of a single ODL node, and not of the etcd cluster; which is going to have a separate lifecycle).  Perhaps longer term, having an option to keep certain sub-tress only in-memory and not persisted to disk could be brought up with the etcd community as a possible feature request, purely as a performance optimization. For short and even medium term for ODL etcd adopters, this should not be a blocking issue.

* _But what about the EntityOwnershipService, EOS?_ It should be possible to implement it on to of etcd's Lock API, but this is still TBD.  Help most welcome!

* _But what about remote RPCs?_ Dunno.  Needs more thought and POC, discussions... TBD.

* _But I love ODL’s current datastore!_ This project, if successful, will be an alternative to and existing in parallel with the current implementation likely for a long time.


### About alternatives

* _Why not XYZ as a KV DB?_ There are a number of other Key Value DBs.  Some of the code from this project likely is a good basis for you to write adapters from YANG to other KV DBs.  Have fun!

* _Why not just replace the Akka persistence LevelDB plugin currently used in CDS to something else like MongoDB?_  : Not sure that I see how an architecture where you keep Akka and would replace LevelDB (a node local single user embedded KV) to a remote multi user DB makes much sense.  Using something like MongoDB as a data store like this project does for etcd perhaps makes more sense; see question above.  You can, of course, just use MongoDB or whatever else you like (JDBC if you must, anything you fancy...) INSTEAD of a YANG-based data store, if that meets your requirements.


### About etcd

* _What is etcd?_ [etcd](https://coreos.com/etcd/) is a distributed [key value store](https://en.wikipedia.org/wiki/Key-value_database) that provides a reliable way to store data across a cluster of machines.  Communication between etcd machines is handled via the Raft consensus algorithm.

* _Why etcd?_ Among many other users, etcd is the database used in Kubernetes (and its distributions such as OpenShift).  It makes sense to align ODL to this.  With the Core OS acquisition, Red Hat has etcd expertise.

* _I [read somewhere online](https://coreos.com/etcd/docs/latest/learning/api_guarantees.html) that "etcd clients may have issues with operations that time out (network disruption for example) and will not send an abort respond". How do we plan on dealing with this?_  This will cause a timeout at [the GRPC layer](https://grpc.io) internally, which will lead to a failure on the MD SAL (commit) operation, which will be propagated to the ODL application client - as it should; all good.

* _I heard that "On network split (AKA split brain) read request may be served mistakenly by the minority split." How do we plan on dealing with this?_ [According to this documentation](https://github.com/coreos/etcd/blob/master/Documentation/op-guide/failures.md), "there is no 'split-brain' in etcd".

* _But, but, but..._ Please consult with the opensource etcd community, or obtain professional support, for further doubts about and issues with etcd - just like you would say in OpenStack if you had a problem with its MariaDB (mysql) database.  Relying on a well established and here-to-stay persistence engine, instead of building, debugging and maintaining a home grown one, is really the main point of this project! ;-)
