This project started as a personal POC by Michael Vorburger.ch to
evaluate the feasibility of using etcd as data store for OpenDaylight (ODL),
instead of its current "home made" CDS based on Akka.

The plan is contribute this to opendaylight.org, if and when successful.

The current status is that of a pre-alpha 0.0.0.1; it CAN (just barely)
PUT a YANG DOM data object into etcd, but does not yet handle trees correctly,
is not transactional, etc.
