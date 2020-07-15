package validations

import exception.InvalidInputException
import models.{HoursSchedule, WeeklySchedule}
import org.junit.Assert.assertTrue
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.Injecting

class OpenHoursScheduleValidatorSpec extends PlaySpec with GuiceOneAppPerTest with Injecting with MockitoSugar {

  private val validator = new OpenHoursScheduleValidatorImpl
  private val scheduleError =
    WeeklySchedule(monday = Some(Seq(HoursSchedule("open", 3600), HoursSchedule("close", -1))),
      tuesday = None,
      wednesday = None,
      thursday = None,
      friday = None,
      saturday = None,
      sunday = None)
  private val scheduleSuccess = scheduleError.copy(monday = Some(Seq(HoursSchedule("open", 3600), HoursSchedule("close", 18000))))
  private val unorderedRange = Seq(HoursSchedule("open", 3600), HoursSchedule("close", 18000),
    HoursSchedule("open", 16000), HoursSchedule("close", 20000))
  private val orderedRange = Seq(HoursSchedule("open", 3600), HoursSchedule("close", 18000),
    HoursSchedule("open", 19000), HoursSchedule("close", 20000))
  private val closedBeforeOpen = Seq(HoursSchedule("open", 3600), HoursSchedule("close", 0))

  "validateTime()" should {

    "Throw exception if one or more time(s) is/are less than min value" in {
      try {
        validator.validateTime(scheduleError)
        fail("Should not have succeeded")
      } catch {
        case e: InvalidInputException => e.getMessage must equal(s"Time -1 outside valid ${0 to 86399}")
      }
    }

    "Throw exception if one or more time(s) is/are more than max value" in {
      try {
        validator.validateTime(scheduleError.copy(monday = Some(Seq(HoursSchedule("open", 3600), HoursSchedule("close", 87000)))))
        fail("Should not have succeeded")
      } catch {
        case e: InvalidInputException => e.getMessage must equal(s"Time 87000 outside valid ${0 to 86399}")
      }
    }

    "Should success if times are within range" in {
      try {
        validator.validateTime(scheduleSuccess)
        assertTrue(true)
      } catch {
        case e: InvalidInputException => fail("Should not have failed with exception : " + e)
      }
    }
  }

  "validateTimesAreInOrder()" should {


    "Throw exception if ranges are not in order" in {
      try {
        validator.validateTimesAreInOrder(unorderedRange)
        fail("Should not have succeeded")
      } catch {
        case e: InvalidInputException => e.getMessage must equal("One (or maybe more) opening closing hours are not in order for a particular day")
      }
    }

    "Should success if ranges are in order" in {
      try {
        validator.validateTimesAreInOrder(orderedRange)
        assertTrue(true)
      } catch {
        case e: InvalidInputException => fail("Should not have failed with exception : " + e)
      }
    }
  }


  "validateRangePairs" should {

    "Throw exception if range pairs have overlapping times" in {
      try {
        validator.validateRangePairs(closedBeforeOpen)
        fail("Should not have succeeded")
      } catch {
        case e: InvalidInputException =>
          e.getMessage must equal("Closing time is before for this pair : open=3600, close=0")
      }
    }

  }

  "Should allow if only open time defined" in {
    try {
      validator.validateRangePairs(Seq(HoursSchedule("open", 3600)))
      assertTrue(true)
    } catch {
      case e: InvalidInputException => fail("Should not have failed with exception : " + e)
    }
  }

  "Should allow valid ranges" in {
    try {
      validator.validateRangePairs(orderedRange)
      assertTrue(true)
    } catch {
      case e: InvalidInputException => fail("Should not have failed with exception : " + e)
    }
  }

  "Should allow empty ranges" in {
    try {
      validator.validateRangePairs(Seq.empty[HoursSchedule])
      assertTrue(true)
    } catch {
      case e: InvalidInputException => fail("Should not have failed with exception : " + e)
    }
  }

}
