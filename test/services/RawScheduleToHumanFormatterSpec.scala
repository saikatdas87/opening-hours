package services

import java.text.DateFormatSymbols
import java.util.Locale

import models.OpenDuration
import org.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.test.Injecting

class RawScheduleToHumanFormatterSpec extends PlaySpec with GuiceOneAppPerTest with Injecting with MockitoSugar {

  private val dateTimeService = mock[DateTimeAndLocaleService]
  private val humanFormatter = new RawScheduleToHumanFormatterImpl(dateTimeService)
  "rawScheduleToHumanText()" should {

    "convert rawScheduleMap to string" in {
      implicit val locale: Locale = Locale.ENGLISH
      val rawMap = Map(
        "Monday" -> Some(Seq(OpenDuration(3600, 18000))),
        "Tuesday" -> None,
        "Wednesday" -> None,
        "Thursday" -> None,
        "Friday" -> None,
        "Saturday" -> None,
        "Sunday" -> None
      )
      val expected =
        """Monday: 1 AM - 5 AM
          |Tuesday: Closed
          |Wednesday: Closed
          |Thursday: Closed
          |Friday: Closed
          |Saturday: Closed
          |Sunday: Closed""".stripMargin

      when(dateTimeService.getLocaleWeekDays) thenReturn new DateFormatSymbols(locale).getWeekdays
      when(dateTimeService.unixTimeToHuman(3600)) thenReturn "1 AM"
      when(dateTimeService.unixTimeToHuman(18000)) thenReturn "5 AM"

      val humanString = humanFormatter.rawScheduleToHumanText(rawMap)

      humanString must equal(expected)
    }

    "convert empty rawScheduleMap to empty string" in {
      implicit val locale: Locale = Locale.ENGLISH
      val rawMap = Map.empty[String, Option[Seq[OpenDuration]]]

      when(dateTimeService.getLocaleWeekDays) thenReturn new DateFormatSymbols(locale).getWeekdays
      when(dateTimeService.unixTimeToHuman(3600)) thenReturn "1 AM"
      when(dateTimeService.unixTimeToHuman(18000)) thenReturn "5 AM"

      val humanString = humanFormatter.rawScheduleToHumanText(rawMap)

      humanString must equal("")
    }

  }
}
