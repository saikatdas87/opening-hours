package services

import java.text.DateFormatSymbols
import java.util.Locale

import com.google.inject.{ImplementedBy, Inject}
import com.typesafe.config.Config
import exception.InvalidInputException
import models.{HoursSchedule, OpenDuration, WeeklySchedule}
import play.api.Configuration

import scala.util.{Failure, Success, Try}

@ImplementedBy(classOf[WeekScheduleFormatterImpl])
trait WeekScheduleFormatter {
  def formatToHuman(schedule: WeeklySchedule): String
}

sealed trait WeekSchedulerProcessorException extends Exception

case object MalformedInputException extends WeekSchedulerProcessorException

class WeekScheduleFormatterImpl @Inject()(configuration: Configuration) extends WeekScheduleFormatter {

  private lazy val applicationConfig: Config = configuration.underlying
  private lazy final val defaultLocale: String = "en"
  private lazy val invalidInput = InvalidInputException("Invalid open and close time sequence observed")

  override def formatToHuman(schedule: WeeklySchedule): String = {
    processRange(schedule)
    "Sample ???"
  }


  private def processRange(schedule: WeeklySchedule): Map[String, Option[Seq[Try[OpenDuration]]]] = {
    val ranges = orderInputToSortedDailySchedules(schedule)
    val groupedRanges = groupSchedules(ranges)

    if (groupedRanges.flatten.forall(_.isSuccess)) {
      val configLocale = applicationConfig.getString("locale")
      val locale = new Locale(if (configLocale != null && !configLocale.isEmpty) configLocale else defaultLocale)
      val weekdays = new DateFormatSymbols(locale).getWeekdays

      val weeklyScheduleMap = for {
        (dailyOpenDurations, index) <- groupedRanges.zipWithIndex
      } yield {
        weekdays((index + 1) % 7 + 1) -> (if (dailyOpenDurations.nonEmpty) Some(dailyOpenDurations) else None)
      }

      weeklyScheduleMap.toMap
    } else {
      throw invalidInput
    }

  }


  private def orderInputToSortedDailySchedules(schedule: WeeklySchedule): Seq[(Seq[HoursSchedule], Seq[HoursSchedule])] = {
    val (maybeNextDayClosing, dailySchedule) = schedule.sortedDays.map(_.getOrElse(Seq.empty)).map {
      case ranges if ranges.nonEmpty && ranges.head.isClosingTime => (Seq(ranges.head), ranges.tail)
      case ranges => (Seq.empty, ranges)
    }.unzip

    // In some cases when closing time fall next day those times are stored in maybeNextDayClosing
    // Need to left shift this seq so that we can zip close time with previous day
    // Here problem is if the closing time not in next day but next to next.What happens then ? We need to redesign input maybe.
    val rearrangedPrevDayClosingSeq = rotateSeqLeft(maybeNextDayClosing)

    dailySchedule.zip(rearrangedPrevDayClosingSeq)
  }

  private def groupSchedules(rangeTuples: Seq[(Seq[HoursSchedule], Seq[HoursSchedule])]): Seq[Seq[Try[OpenDuration]]] = {
    for {
      daily <- rangeTuples.map(pair => pair._1 ++ pair._2)
    } yield {
      daily.grouped(2).map {
        case Seq(open, close) if open.isOpeningTime && close.isClosingTime =>
          Success(OpenDuration(open.value, close.value))
        case _ =>
          Failure(invalidInput)
      }.toVector
    }
  }

  def rotateSeqLeft[T](seq: Seq[T]): Seq[T] = seq.tail :+ seq.head

}
