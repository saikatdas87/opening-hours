package services

import java.text.DateFormatSymbols
import java.util.Locale

import exception.InvalidInputException
import models.{HoursSchedule, WeeklySchedule}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.{doNothing, when}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.Injecting
import validations.OpenHoursScheduleValidator

class WeekScheduleFormatterSpec extends PlaySpec with GuiceOneAppPerTest with Injecting with MockitoSugar {

  private val validator = mock[OpenHoursScheduleValidator]
  private val dateTimeService = mock[DateTimeAndLocaleService]
  private val formatter = new WeekScheduleFormatterImpl(dateTimeService, validator)
  private val schedule = WeeklySchedule(
    monday = Some(Seq(HoursSchedule("open", 3600), HoursSchedule("close", 18000))),
    tuesday = None,
    wednesday = None,
    thursday = None,
    friday = None,
    saturday = None,
    sunday = None)


  "formatToHuman()" should {

    "returns successfully human readable string for valid input " in {
      implicit val locale: Locale = Locale.ENGLISH
      val expected =
        """Monday: 1 AM - 5 AM
          |Tuesday: Closed
          |Wednesday: Closed
          |Thursday: Closed
          |Friday: Closed
          |Saturday: Closed
          |Sunday: Closed""".stripMargin

      doNothing().when(validator).validateTime(schedule)
      doNothing().when(validator).validateRangePairs(any[Seq[HoursSchedule]])
      doNothing().when(validator).validateTimesAreInOrder(any[Seq[HoursSchedule]])

      when(dateTimeService.getLocaleWeekDays) thenReturn new DateFormatSymbols(locale).getWeekdays
      when(dateTimeService.unixTimeToHuman(3600)) thenReturn "1 AM"
      when(dateTimeService.unixTimeToHuman(18000)) thenReturn "5 AM"


      val res = formatter.formatToHuman(schedule)

      res must equal(expected)
    }

    "returns successfully human readable string for valid input in other locale" in {
      implicit val locale: Locale = new Locale("fi")
      val expected =
        """maanantai: 1 AM - 5 AM
          |tiistai: Closed
          |keskiviikko: Closed
          |torstai: Closed
          |perjantai: Closed
          |lauantai: Closed
          |sunnuntai: Closed""".stripMargin

      doNothing().when(validator).validateTime(schedule)
      doNothing().when(validator).validateRangePairs(any[Seq[HoursSchedule]])
      doNothing().when(validator).validateTimesAreInOrder(any[Seq[HoursSchedule]])

      when(dateTimeService.getLocaleWeekDays) thenReturn new DateFormatSymbols(locale).getWeekdays
      when(dateTimeService.unixTimeToHuman(3600)) thenReturn "1 AM"
      when(dateTimeService.unixTimeToHuman(18000)) thenReturn "5 AM"


      val res = formatter.formatToHuman(schedule)

      res must equal(expected)
    }


    "throws exception if any validation fails" in {
      implicit val locale: Locale = new Locale("fi")
      when(validator.validateTime(any())) thenThrow InvalidInputException("Outside range")

      try {
        formatter.formatToHuman(schedule)
        fail("Must have not succeeded")
      } catch {
        case e: InvalidInputException => e.getMessage must equal("Outside range")
      }
    }

    "throws exception if service throws exceptions" in {
      implicit val locale: Locale = new Locale("fi")
      doNothing().when(validator).validateTime(schedule)
      doNothing().when(validator).validateRangePairs(any[Seq[HoursSchedule]])
      doNothing().when(validator).validateTimesAreInOrder(any[Seq[HoursSchedule]])
      when(dateTimeService.getLocaleWeekDays) thenThrow new RuntimeException("Something bad happened")

      try {
        formatter.formatToHuman(schedule)
        fail("Must have not succeeded")
      } catch {
        case e: Exception => e.getMessage must equal("Something bad happened")
      }
    }

  }
}
