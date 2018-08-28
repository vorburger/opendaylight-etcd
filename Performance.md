# opendaylight-etcd Performance

## Size of KVs

### How to convert a dump of snapshots/ and journal/ from CDS from a scale lab test to etcd KVs to guestimate expected size

    cd ODL/git/netvirt/karaf/
    mvn clean package
    ./target/assembly/bin/karaf
    opendaylight-user@root>feature:install odl-netvirt-openstack
    opendaylight-user@root>CTRL-C
    trash target/assembly/snapshots/ target/assembly/journal
    cp -R ....scale-lab-dump/logsToMichaelAug28/controller-0/* target/assembly