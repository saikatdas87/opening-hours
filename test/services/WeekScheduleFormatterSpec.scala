package services

import java.text.DateFormatSymbols
import java.util.Locale

import exception.InvalidInputException
import models.{HoursSchedule, OpenDuration, WeeklySchedule}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.Injecting
import validations.OpenHoursScheduleValidator

import scala.util.{Failure, Success}

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


  "processRangeToRawScheduleMap()" should {

    "returns successfully raw Schedule map for valid input " in {
      implicit val locale: Locale = Locale.ENGLISH
      val expected = Map(
        "Monday" -> Some(Seq(Success(OpenDuration(3600, 18000)))),
        "Tuesday" -> None,
        "Wednesday" -> None,
        "Thursday" -> None,
        "Friday" -> None,
        "Saturday" -> None,
        "Sunday" -> None
      )


      when(dateTimeService.getLocaleWeekDays) thenReturn new DateFormatSymbols(locale).getWeekdays

      val res = formatter.processRangeToRawScheduleMap(schedule)

      res must equal(expected)
    }

    "returns successfully raw Schedule map in other locale" in {
      implicit val locale: Locale = new Locale("fi")
      val expected = Map(
        "maanantai" -> Some(Seq(Success(OpenDuration(3600, 18000)))),
        "tiistai" -> None,
        "keskiviikko" -> None,
        "torstai" -> None,
        "perjantai" -> None,
        "lauantai" -> None,
        "sunnuntai" -> None
      )

      when(dateTimeService.getLocaleWeekDays) thenReturn new DateFormatSymbols(locale).getWeekdays

      val res = formatter.processRangeToRawScheduleMap(schedule)

      res must equal(expected)
    }

    "returns successfully raw Schedule map wrapping failure " in {
      implicit val locale: Locale = new Locale("fi")
      val expected = Map(
        "maanantai" -> Some(Seq(Failure(InvalidInputException("Invalid open and close time sequence observed")))),
        "tiistai" -> None,
        "keskiviikko" -> None,
        "torstai" -> None,
        "perjantai" -> None,
        "lauantai" -> None,
        "sunnuntai" -> None
      )

      when(dateTimeService.getLocaleWeekDays) thenReturn new DateFormatSymbols(locale).getWeekdays

      val res = formatter.processRangeToRawScheduleMap(schedule.copy(monday = Some(Seq(HoursSchedule("open", 3600)))))

      res must equal(expected)
    }

    "returns successfully raw Schedule map wrapping failure for consecutive open type only" in {
      implicit val locale: Locale = new Locale("fi")
      val expected = Map(
        "maanantai" -> Some(Seq(Failure(InvalidInputException("Invalid open and close time sequence observed")))),
        "tiistai" -> None,
        "keskiviikko" -> None,
        "torstai" -> None,
        "perjantai" -> None,
        "lauantai" -> None,
        "sunnuntai" -> None
      )

      when(dateTimeService.getLocaleWeekDays) thenReturn new DateFormatSymbols(locale).getWeekdays

      val res = formatter.processRangeToRawScheduleMap(schedule.copy(monday = Some(Seq(HoursSchedule("open", 3600), HoursSchedule("open", 360000)))))

      res must equal(expected)
    }


    "returns successfully human readable string for valid input in other locale" in {
      implicit val locale: Locale = new Locale("fi")
      val expected = Map(
        "maanantai" -> Some(Seq(Success(OpenDuration(3600, 18000)))),
        "tiistai" -> None,
        "keskiviikko" -> None,
        "torstai" -> None,
        "perjantai" -> None,
        "lauantai" -> None,
        "sunnuntai" -> None
      )

      when(dateTimeService.getLocaleWeekDays) thenReturn new DateFormatSymbols(locale).getWeekdays

      val res = formatter.processRangeToRawScheduleMap(schedule)

      res must equal(expected)
    }


    "throws exception if any validation fails" in {
      implicit val locale: Locale = new Locale("fi")
      when(validator.validateTime(any())) thenThrow InvalidInputException("Something bad happened")

      try {
        formatter.processRangeToRawScheduleMap(schedule)
        fail("Must have not succeeded")
      } catch {
        case e: InvalidInputException => e.getMessage must equal("Something bad happened")
      }
    }

    "throws exception if service throws exceptions" in {
      implicit val locale: Locale = new Locale("fi")

      when(dateTimeService.getLocaleWeekDays) thenThrow new RuntimeException("Something bad happened")

      try {
        formatter.processRangeToRawScheduleMap(schedule)
        fail("Must have not succeeded")
      } catch {
        case e: RuntimeException => e.getMessage must equal("Something bad happened")
      }
    }

  }


}
