package net.elenx.hd.akka.sample.iot.group.device

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.testkit.javadsl.TestKit
import net.elenx.hd.akka.sample.iot.group.DeviceRegistered
import net.elenx.hd.akka.sample.iot.group.RequestTrackDevice
import org.junit.AfterClass
import org.junit.Assert
import org.junit.BeforeClass
import org.junit.Test
import java.util.*

class DeviceActorTest
{
    companion object
    {
        private const val MESSAGE_ID = 42L

        private const val GROUP_ID = "43110"
        private const val DEVICE_ID = "1337"

        private const val WRONG_GROUP_ID = "666"
        private const val WRONG_DEVICE_ID = "999"

        lateinit var system: ActorSystem

        @BeforeClass
        @JvmStatic
        fun setup()
        {
            system = ActorSystem.create()
        }

        @AfterClass
        @JvmStatic
        fun teardown()
        {
            akka.testkit.javadsl.TestKit.shutdownActorSystem(system)
        }
    }

    @Test
    fun shouldReplyWithNoKnownTemperature()
    {
        //given
        val mockActor = TestKit(system)
        val deviceActor = system.actorOf(DeviceActor.props(GROUP_ID, DEVICE_ID))

        //when
        deviceActor.tell(ReadTemperature(MESSAGE_ID), mockActor.ref)

        val response = mockActor.expectMsgClass(RespondTemperature::class.java)

        //then
        Assert.assertEquals(MESSAGE_ID, response.requestId)
        Assert.assertEquals(Optional.empty<Double>(), response.value)

    }

    @Test
    fun shouldReplyWithRecordedTemperature()
    {
        //given
        val mockActor = TestKit(system)
        val deviceActor = system.actorOf(DeviceActor.props(GROUP_ID, DEVICE_ID))
        val recordedTemperature = 451.0

        //when
        deviceActor.tell(RecordTemperature(1L, recordedTemperature), ActorRef.noSender())
        deviceActor.tell(ReadTemperature(MESSAGE_ID), mockActor.ref)

        val response = mockActor.expectMsgClass(RespondTemperature::class.java)

        //then
        Assert.assertTrue(response.value.isPresent)
        Assert.assertEquals(Optional.of(recordedTemperature), response.value)

    }

    @Test
    fun testReplyWithLatestTemperatureReading()
    {
        //given
        val probe = TestKit(system)
        val deviceActor = system.actorOf(DeviceActor.props(GROUP_ID, DEVICE_ID))
        val lastRecordedTemperature = 55.0

        //when
        deviceActor.tell(RecordTemperature(1L, 24.0), ActorRef.noSender())
        deviceActor.tell(RecordTemperature(2L, lastRecordedTemperature), ActorRef.noSender())

        deviceActor.tell(ReadTemperature(MESSAGE_ID), probe.ref)
        val response = probe.expectMsgClass(RespondTemperature::class.java)

        //then
        Assert.assertEquals(MESSAGE_ID, response.requestId)
        Assert.assertEquals(Optional.of(lastRecordedTemperature), response.value)

    }

    @Test
    fun testReplyToRegistrationRequests()
    {
        //given
        val mockActor = TestKit(system)
        val deviceActor = system.actorOf(DeviceActor.props(GROUP_ID, DEVICE_ID))

        //when
        deviceActor.tell(RequestTrackDevice(MESSAGE_ID, GROUP_ID, DEVICE_ID), mockActor.ref)

        //then
        mockActor.expectMsgClass(DeviceRegistered::class.java)
        Assert.assertEquals(deviceActor, mockActor.lastSender)

    }

    @Test
    fun testIgnoreWrongRegistrationRequests()
    {
        //given
        val mockActor = TestKit(system)
        val deviceActor = system.actorOf(DeviceActor.props(GROUP_ID, DEVICE_ID))

        //when //then
        deviceActor.tell(RequestTrackDevice(MESSAGE_ID, WRONG_GROUP_ID, DEVICE_ID), mockActor.ref)
        mockActor.expectNoMsg()

        deviceActor.tell(RequestTrackDevice(MESSAGE_ID, GROUP_ID, WRONG_DEVICE_ID), mockActor.ref)
        mockActor.expectNoMsg()

    }

}