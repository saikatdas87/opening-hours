package services

import java.util.Locale

import com.google.inject.ImplementedBy
import javax.inject.Inject
import models.OpenDuration

@ImplementedBy(classOf[RawScheduleToHumanFormatterImpl])
trait RawScheduleToHumanFormatter {
  def rawScheduleToHumanText(rawScheduleMap: Map[String, Option[Seq[OpenDuration]]])(implicit locale: Locale): String
}


class RawScheduleToHumanFormatterImpl @Inject()(dateTimeAndLocaleService: DateTimeAndLocaleService) extends RawScheduleToHumanFormatter {

  def rawScheduleToHumanText(rawScheduleMap: Map[String, Option[Seq[OpenDuration]]])(implicit locale: Locale): String = {
    val weekdays = dateTimeAndLocaleService.getLocaleWeekDays
    val humanString = for {
      day <- weekdays.drop(2) :+ weekdays(1) if rawScheduleMap.contains(day) // Starting from Monday
      maybeOpenHours <- rawScheduleMap.get(day)
    } yield {
      day + ": " + {
        maybeOpenHours match {
          case Some(openHours) =>
            openHours.map { range =>
              s"${dateTimeAndLocaleService.unixTimeToHuman(range.start)} - ${dateTimeAndLocaleService.unixTimeToHuman(range.end)}"
            }.mkString(", ")
          case None => "Closed"
        }
      }
    }
    humanString.mkString("\n")
  }
}
