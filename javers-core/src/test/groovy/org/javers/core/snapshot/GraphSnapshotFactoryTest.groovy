package org.javers.core.snapshot

import org.javers.core.JaversTestBuilder
import org.javers.core.model.DummyAddress
import org.javers.core.model.SnapshotEntity
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static org.javers.core.snapshot.SnapshotsAssert.getAssertThat

/**
 * @author bartosz walacik
 */
class GraphSnapshotFactoryTest extends Specification {

    @Shared JaversTestBuilder javers

    def setup(){
        javers = JaversTestBuilder.javersTestAssembly()
    }

    def "should flatten straight Entity relation"() {
        given:
        def cdo = new SnapshotEntity(id: 1, entityRef: new SnapshotEntity(id: 5))
        def node = javers.createObjectGraph(cdo)

        when:
        List snapshots = javers.graphSnapshotFactory.create(node)

        then:
        assertThat(snapshots).hasSize(2)
                             .hasSnapshot(javers.instanceId(1, SnapshotEntity))
                             .hasSnapshot(javers.instanceId(5, SnapshotEntity))
    }

    def "should flatten graph with depth 2"(){
        given:
        def ref3  = new SnapshotEntity(id:3)
        def ref2  = new SnapshotEntity(id:2,entityRef: ref3)
        //cdo -> ref2 -> ref3
        def cdo   = new SnapshotEntity(id:1,entityRef: ref2)
        def node = javers.createObjectGraph(cdo)

        when:
        List snapshots = javers.graphSnapshotFactory.create(node)

        then:
        assertThat(snapshots).hasSize(3)
                             .hasSnapshot(javers.instanceId(1, SnapshotEntity))
                             .hasSnapshot(javers.instanceId(2, SnapshotEntity))
                             .hasSnapshot(javers.instanceId(3, SnapshotEntity))
    }

    def "should flatten straight ValueObject relation"() {
        given:
        def cdo  = new SnapshotEntity(id:1, valueObjectRef: new DummyAddress("street"))
        def node = javers.createObjectGraph(cdo)

        when:
        List snapshots = javers.graphSnapshotFactory.create(node)

        then:
        assertThat(snapshots).hasSize(2)
                             .hasSnapshot(javers.instanceId(1, SnapshotEntity))
                             .hasSnapshot(javers.voBuilder(1, SnapshotEntity).voId(DummyAddress,"valueObjectRef"))
    }

    def "should flatten Set of ValueObject"() {
        given:
        def cdo = new SnapshotEntity(setOfValueObjects: [new DummyAddress("London"), new DummyAddress("London City")])
        def node = javers.createObjectGraph(cdo)

        when:
        List snapshots = javers.graphSnapshotFactory.create(node)

        then:
        assertThat(snapshots).hasSize(3)
                             .hasSnapshot(javers.instanceId(1, SnapshotEntity))
                             .hasSnapshot(javers.voBuilder(1, SnapshotEntity).voId(DummyAddress,"setOfValueObjects/random_0"))
                             .hasSnapshot(javers.voBuilder(1, SnapshotEntity).voId(DummyAddress,"setOfValueObjects/random_1"))

    }

    @Unroll
    def "should flatten #listType of ValueObject"() {
        given:
        def node = javers.createObjectGraph(cdo)

        when:
        List snapshots = javers.graphSnapshotFactory.create(node)

        then:
        assertThat(snapshots).hasSize(3)
                             .hasSnapshot(javers.instanceId(1, SnapshotEntity))
                             .hasSnapshot(expectedVoIds[0])
                             .hasSnapshot(expectedVoIds[1])

        where:
        listType << ["List", "Array"]
        cdo <<      [new SnapshotEntity(listOfValueObjects:  [new DummyAddress("London"), new DummyAddress("London City")]),
                     new SnapshotEntity(arrayOfValueObjects: [new DummyAddress("London"), new DummyAddress("London City")])
                    ]
        expectedVoIds << [
                    [javers.voBuilder(1, SnapshotEntity).voId(DummyAddress,"listOfValueObjects/0"),
                     javers.voBuilder(1, SnapshotEntity).voId(DummyAddress,"listOfValueObjects/1")],
                    [javers.voBuilder(1, SnapshotEntity).voId(DummyAddress,"arrayOfValueObjects/0"),
                     javers.voBuilder(1, SnapshotEntity).voId(DummyAddress,"arrayOfValueObjects/1")]
                    ]

    }

    @Unroll
    def "should flatten #containerType of Entity"() {
        given:
        def node = javers.createObjectGraph(cdo)

        when:
        List snapshots = javers.graphSnapshotFactory.create(node)

        then:
        assertThat(snapshots).hasSize(3)
                             .hasSnapshot(javers.instanceId(1, SnapshotEntity))
                             .hasSnapshot(javers.instanceId(5, SnapshotEntity))
                             .hasSnapshot(javers.instanceId(6, SnapshotEntity))

        where:
        containerType << ["List", "Array", "Set"]
        cdo <<      [new SnapshotEntity(listOfEntities:  [new SnapshotEntity(id:5), new SnapshotEntity(id:6)]),
                     new SnapshotEntity(arrayOfEntities: [new SnapshotEntity(id:5), new SnapshotEntity(id:6)]),
                     new SnapshotEntity(setOfEntities:   [new SnapshotEntity(id:5), new SnapshotEntity(id:6)])
                    ]
    }

    @Unroll
    def "should flatten Map of <#keyType, #valueType>"() {
        given:
        def node = javers.createObjectGraph(cdo)

        when:
        List snapshots = javers.graphSnapshotFactory.create(node)

        then:
        assertThat(snapshots).hasSize(3)
                             .hasSnapshot(javers.instanceId(1, SnapshotEntity))
                             .hasSnapshot(expectedVoIds[0])
                             .hasSnapshot(expectedVoIds[1])

        where:
        keyType <<   ["Entity", "Primitive"]
        valueType << ["Entity", "ValueObject"]
        propertyName <<  ["mapOfEntities","mapPrimitiveToVO"]
        cdo << [
                new SnapshotEntity(mapOfEntities:    [(new SnapshotEntity(id:2)): new SnapshotEntity(id:3)]),
                new SnapshotEntity(mapPrimitiveToVO: ["key1": new DummyAddress("London"), "key2": new DummyAddress("City")])
        ]
        expectedVoIds << [ [javers.instanceId(2, SnapshotEntity),javers.instanceId(3, SnapshotEntity)],
                           [javers.voBuilder(1, SnapshotEntity).voId(DummyAddress,"mapPrimitiveToVO/key1"),
                            javers.voBuilder(1, SnapshotEntity).voId(DummyAddress,"mapPrimitiveToVO/key2")]
                         ]
    }
}
