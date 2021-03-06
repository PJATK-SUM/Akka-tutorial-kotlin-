package net.elenx.hd.akka.sample.iot.manager.group

import akka.testkit.javadsl.TestKit
import net.elenx.hd.akka.sample.iot.manager.group.device.RecordTemperature
import net.elenx.hd.akka.sample.iot.manager.group.device.TemperatureRecorded
import net.elenx.hd.akka.sample.iot.manager.group.protocol.DeviceRegistered
import net.elenx.hd.akka.sample.iot.manager.group.protocol.RequestTrackDevice
import net.elenx.hd.akka.sample.test.support.ActorSystemTestBase
import org.junit.Assert
import org.junit.Test

class DeviceGroupActorDeviceRegistrationTest : ActorSystemTestBase()
{
    companion object
    {
        private const val DEVICE_GROUP_ID_HALL = "hall"
        private const val CEILING_THERMOMETER_ID_HALL = "thermometer-ceiling"
        private const val WALL_THERMOMETER_ID_HALL = "thermometer-wall"

        private const val DEVICE_GROUP_ID_GARAGE = "garage"
    }

    @Test
    fun shouldIgnoreRequestsForWrongGroupId()
    {
        //given
        val mockActor = TestKit(system)
        val groupActor = system.actorOf(DeviceGroupActor.props(DEVICE_GROUP_ID_HALL))

        //when
        groupActor.tell(RequestTrackDevice(1L, DEVICE_GROUP_ID_GARAGE, CEILING_THERMOMETER_ID_HALL), mockActor.ref)

        //then
        mockActor.expectNoMsg()

    }

    @Test
    fun shouldRegisterDeviceActor()
    {
        //given
        val mockActor = TestKit(system)
        val groupActor = system.actorOf(DeviceGroupActor.props(DEVICE_GROUP_ID_HALL))
        val requestTrackHallThermometer = RequestTrackDevice(0L, DEVICE_GROUP_ID_HALL, CEILING_THERMOMETER_ID_HALL)

        //when
        groupActor.tell(requestTrackHallThermometer, mockActor.ref)

        //then
        Assert.assertNotNull(mockActor.expectMsgClass(DeviceRegistered::class.java))

    }

    @Test
    fun shouldReturnDeviceActorReference()
    {
        //given
        val mockActor = TestKit(system)
        val groupActor = system.actorOf(DeviceGroupActor.props(DEVICE_GROUP_ID_HALL))
        val requestTrackHallThermometer = RequestTrackDevice(0L, DEVICE_GROUP_ID_HALL, CEILING_THERMOMETER_ID_HALL)

        //when
        groupActor.tell(requestTrackHallThermometer, mockActor.ref)
        mockActor.expectMsgClass(DeviceRegistered::class.java)
        val registeredDeviceActor = mockActor.lastSender

        registeredDeviceActor.tell(RecordTemperature(1L, 1.0), mockActor.ref)

        //then
        Assert.assertNotNull(mockActor.expectMsgClass(TemperatureRecorded::class.java))

    }

    @Test
    fun shouldReturnSameActorForSameDevice()
    {
        //given
        val mockActor = TestKit(ActorSystemTestBase.system)
        val groupActor = ActorSystemTestBase.system.actorOf(DeviceGroupActor.props(DEVICE_GROUP_ID_HALL))

        //when
        groupActor.tell(RequestTrackDevice(0L, DEVICE_GROUP_ID_HALL, CEILING_THERMOMETER_ID_HALL), mockActor.ref)
        mockActor.expectMsgClass(DeviceRegistered::class.java)
        val firstThermometerActor = mockActor.lastSender

        groupActor.tell(RequestTrackDevice(1L, DEVICE_GROUP_ID_HALL, CEILING_THERMOMETER_ID_HALL), mockActor.ref)
        mockActor.expectMsgClass(DeviceRegistered::class.java)
        val secondThermometerActor = mockActor.lastSender

        //then
        Assert.assertEquals(firstThermometerActor, secondThermometerActor)

    }

    @Test
    fun shouldReturnDifferentDevices()
    {
        //given
        val mockActor = TestKit(system)
        val groupActor = system.actorOf(DeviceGroupActor.props(DEVICE_GROUP_ID_HALL))

        //when
        groupActor.tell(RequestTrackDevice(1L, DEVICE_GROUP_ID_HALL, CEILING_THERMOMETER_ID_HALL), mockActor.ref)
        mockActor.expectMsgClass(DeviceRegistered::class.java)
        val ceilingThermometerActor = mockActor.lastSender

        groupActor.tell(RequestTrackDevice(2L, DEVICE_GROUP_ID_HALL, WALL_THERMOMETER_ID_HALL), mockActor.ref)
        mockActor.expectMsgClass(DeviceRegistered::class.java)
        val wallThermometerActor = mockActor.lastSender

        //then
        Assert.assertNotEquals(ceilingThermometerActor, wallThermometerActor)

    }

}