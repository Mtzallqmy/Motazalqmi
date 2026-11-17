package com.moataz.motazalqmi
import com.moataz.motazalqmi.ui.scheduleFromLocal
import com.moataz.motazalqmi.ui.scheduleToLocal
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test
class ScheduleTest {
 @Test fun adenPublicationTimeRoundTrips() {
  val zone=ZoneId.of("Asia/Aden")
  val instant=scheduleFromLocal("2027-01-20 20:30",zone)
  assertEquals("2027-01-20T17:30:00Z",instant)
  assertEquals("2027-01-20 20:30",scheduleToLocal(instant,zone))
  assertEquals("",scheduleFromLocal("",zone))
 }
 @Test fun impossibleCalendarDateCannotPublish() {
  assertTrue(runCatching { scheduleFromLocal("2027-02-30 20:30") }.isFailure)
 }
}
