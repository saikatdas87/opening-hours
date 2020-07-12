package services

import java.text.DateFormatSymbols
import java.util.Locale

import com.google.inject.{ImplementedBy, Inject}
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import exception.InvalidInputException
import models.{HoursSchedule, OpenDuration, WeeklySchedule}
import play.api.Configuration

import scala.util.{Failure, Success, Try}

@ImplementedBy(classOf[WeekScheduleFormatterImpl])
trait WeekScheduleFormatter {
  def formatToHuman(schedule: WeeklySchedule): String
}

class WeekScheduleFormatterImpl @Inject()(configuration: Configuration) extends WeekScheduleFormatter with StrictLogging {

  private lazy val applicationConfig: Config = configuration.underlying
  private val configLocaleKey : String = "locale"
  private lazy final val defaultLocale: Locale = Locale.ENGLISH
  private lazy val invalidInput = InvalidInputException("Invalid open and close time sequence observed")

  override def formatToHuman(schedule: WeeklySchedule): String = {
    processRange(schedule)
    "Sample ???"
  }


  private def processRange(schedule: WeeklySchedule): Map[String, Option[Seq[Try[OpenDuration]]]] = {
    val ranges = orderInputToSortedDailySchedules(schedule)
    val groupedRanges = groupSchedules(ranges)

    if (groupedRanges.flatten.forall(_.isSuccess)) {
      val locale = getLocale
      val weekdays = new DateFormatSymbols(locale).getWeekdays

      val weeklyScheduleMap = for {
        (dailyOpenDurations, index) <- groupedRanges.zipWithIndex
      } yield {
        weekdays((index + 1) % 7 + 1) -> (if (dailyOpenDurations.nonEmpty) Some(dailyOpenDurations) else None)
      }

      weeklyScheduleMap.toMap
    } else {
      //throw invalidInput
      Map.empty
    }

  }


  private def orderInputToSortedDailySchedules(schedule: WeeklySchedule): Seq[(Seq[HoursSchedule], Seq[HoursSchedule])] = {
    val (maybeNextDayClosing, dailySchedule) = schedule.sortedDays.map(_.getOrElse(Seq.empty)).map {
      case ranges if ranges.nonEmpty && ranges.head.isClosingTime => (Seq(ranges.head), ranges.tail)
      case ranges => (Seq.empty, ranges)
    }.unzip

    //TODO: - validate dailySchedule seq to check if close time after open

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
        case seq =>
          logger.error("The range is invalid : " + seq)
          Failure(invalidInput)
      }.toVector
    }
  }

  def rotateSeqLeft[T](seq: Seq[T]): Seq[T] = seq.tail :+ seq.head

  def getLocale: Locale = {
    try {
      val configLocale = applicationConfig.getString(configLocaleKey)
      if (configLocale != null && !configLocale.isEmpty) new Locale(configLocale) else defaultLocale
    } catch {
      case _: Exception =>
        logger.info(s"No Locale config found. Using default locale $defaultLocale for formatting")
        defaultLocale
    }
  }
}
