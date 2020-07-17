package services

import java.util.Locale

import com.google.inject.ImplementedBy
import com.typesafe.scalalogging.StrictLogging
import exception.InvalidInputException
import javax.inject.Inject
import models.{HoursSchedule, OpenDuration, WeeklySchedule}
import validations.OpenHoursScheduleValidator

import scala.util.{Failure, Success, Try}

@ImplementedBy(classOf[WeekScheduleFormatterImpl])
trait WeekScheduleFormatter {
  def processRangeToRawScheduleMap(schedule: WeeklySchedule)(implicit locale: Locale): Map[String, Option[Seq[Try[OpenDuration]]]]
}

class WeekScheduleFormatterImpl @Inject()(dateTimeAndLocaleService: DateTimeAndLocaleService,
                                          validator: OpenHoursScheduleValidator) extends WeekScheduleFormatter with StrictLogging {

  private lazy val invalidInput = InvalidInputException("Invalid open and close time sequence observed")

  private def rotateSeqLeft[T](seq: Seq[T]): Seq[T] = if (seq.nonEmpty) seq.tail :+ seq.head else seq

  def processRangeToRawScheduleMap(schedule: WeeklySchedule)(implicit locale: Locale): Map[String, Option[Seq[Try[OpenDuration]]]] = {
    validator.validateTime(schedule)

    val ranges = orderInputToSortedDailySchedules(schedule)
    val rawGroupedRanges = groupSchedules(ranges)

    val weekdays = dateTimeAndLocaleService.getLocaleWeekDays
    val weeklyScheduleMap = for {
      (dailyOpenDurations, index) <- rawGroupedRanges.zipWithIndex
    } yield {
      weekdays((index + 1) % 7 + 1) -> (if (dailyOpenDurations.nonEmpty) Some(dailyOpenDurations) else None)
    }

    weeklyScheduleMap.toMap
  }


  private def orderInputToSortedDailySchedules(schedule: WeeklySchedule): Seq[(Seq[HoursSchedule], Seq[HoursSchedule])] = {
    val (nextDayClosingHours, rawDailySchedules) = schedule.sortedDays.map(_.getOrElse(Seq.empty)).map {
      case ranges if ranges.nonEmpty && ranges.head.isClosingTime =>
        (Seq(ranges.head), ranges.tail)
      case ranges =>
        (Seq.empty, ranges)
    }.unzip

    // In some cases when closing time fall next day those times are stored in maybeNextDayClosing
    // Need to left shift this seq so that we can zip close time with previous day
    // Here problem is if the closing time not in next day but next to next.What happens then ? We need to redesign input maybe.
    val rearrangedPrevDayClosingSeq = rotateSeqLeft(nextDayClosingHours)

    rawDailySchedules.zip(rearrangedPrevDayClosingSeq)
  }

  private def groupSchedules(rangeWithMaybeNextDayClosingTuples: Seq[(Seq[HoursSchedule], Seq[HoursSchedule])]): Seq[Seq[Try[OpenDuration]]] = {
    for {
      dailyRanges <- rangeWithMaybeNextDayClosingTuples.map(pair => pair._1 ++ pair._2)
    } yield {
      dailyRanges.grouped(2).map {
        case Seq(open, close) if open.isOpeningTime && close.isClosingTime =>
          Success(OpenDuration(open.value, close.value))
        case seq =>
          logger.error("The range/ranges is/are invalid : " + seq)
          // We can throw exception here but if we want to recover still and show valid
          // hours for correct inputs then we can still do with this approach
          Failure(invalidInput)
      }.toVector
    }
  }

}
