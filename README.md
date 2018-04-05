This project started as a personal POC by Michael Vorburger.ch to
evaluate the feasibility of using etcd as data store for OpenDaylight (ODL),
instead of its current "home made" CDS based on Akka.

The plan is contribute this to opendaylight.org, if and when successful.

The current status is that of a pre-alpha 0.0.0.1; it CAN (just barely)
PUT a YANG DOM data object into etcd, but does not yet handle trees correctly,
is not transactional, etc.


*How to build?*

If you're hitting an NPE in org.apache.maven.plugins:maven-javadoc-plugin:3.0.0:jar,
just use `mvn -Dmaven.javadoc.skip=true clean install` to work around it; it's possibly
related to not having JAVA_HOME environment variable (what's weird is that this NPE
does not seem to happen with other OpenDaylight builds).

To get a more recent version of jetcd-core than the currently used 0.0.2, just
`git clone https://github.com/coreos/jetcd.git ; cd jetcd; mvn [-DskipTests] clean install`
and change the dependency to jetcd-core in jetcd/pom.xml from 0.0.2 to 0.1.0-SNAPSHOT.
