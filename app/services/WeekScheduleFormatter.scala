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
  def formatToHuman(schedule: WeeklySchedule)(implicit locale: Locale): String
}

class WeekScheduleFormatterImpl @Inject()(dateTimeAndLocaleService: DateTimeAndLocaleService,
                                          validator: OpenHoursScheduleValidator) extends WeekScheduleFormatter with StrictLogging {

  private lazy val invalidInput = InvalidInputException("Invalid open and close time sequence observed")

  override def formatToHuman(schedule: WeeklySchedule)(implicit locale: Locale): String = {
    val scheduledRangeMap = processRange(schedule)
    formatScheduledMapToHumanText(scheduledRangeMap)
  }


  private def processRange(schedule: WeeklySchedule)(implicit locale: Locale): Map[String, Option[Seq[OpenDuration]]] = {
    validator.validateTime(schedule)
    val ranges = orderInputToSortedDailySchedules(schedule)
    val rawGroupedRanges = groupSchedules(ranges)

    if (rawGroupedRanges.flatten.forall(_.isSuccess)) {
      val groupedRanges = for {
        dailyRanges <- rawGroupedRanges
      } yield {
        dailyRanges.collect({ case Success(range) => range })
      }

      val weekdays = dateTimeAndLocaleService.getLocaleWeekDays
      val weeklyScheduleMap = for {
        (dailyOpenDurations, index) <- groupedRanges.zipWithIndex
      } yield {
        weekdays((index + 1) % 7 + 1) -> (if (dailyOpenDurations.nonEmpty) Some(dailyOpenDurations) else None)
      }

      weeklyScheduleMap.toMap
    } else {
      logger.error("Error occurred while processing raw input")
      throw InvalidInputException("One or more range pairs (type or value) in input are invalid")
    }

  }


  private def orderInputToSortedDailySchedules(schedule: WeeklySchedule): Seq[(Seq[HoursSchedule], Seq[HoursSchedule])] = {
    val (nextDayClosingHours, rawDailySchedules) = schedule.sortedDays.map(_.getOrElse(Seq.empty)).map {
      case ranges if ranges.nonEmpty && ranges.head.isClosingTime =>
        // Validation each day ranges are proper :
        // 1. Schedules for every has pair of open, close
        // 2. closing time after open
        validator.validateRangePairs(ranges.tail)
        // validate order of ranges are correct
        validator.validateTimesAreInOrder(ranges.tail)
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

  private def groupSchedules(rangeTuples: Seq[(Seq[HoursSchedule], Seq[HoursSchedule])]): Seq[Seq[Try[OpenDuration]]] = {
    for {
      daily <- rangeTuples.map(pair => pair._1 ++ pair._2)
    } yield {
      daily.grouped(2).map {
        case Seq(open, close) if open.isOpeningTime && close.isClosingTime =>
          Success(OpenDuration(open.value, close.value))
        case seq =>
          logger.error("The range/ranges is/are invalid : " + seq)
          Failure(invalidInput)
      }.toVector
    }
  }

  def rotateSeqLeft[T](seq: Seq[T]): Seq[T] = if (seq.nonEmpty) seq.tail :+ seq.head else seq

  private def formatScheduledMapToHumanText(schedules: Map[String, Option[Seq[OpenDuration]]])(implicit locale: Locale): String = {
    val weekdays = dateTimeAndLocaleService.getLocaleWeekDays
    val humanString = for {
      day <- weekdays.drop(2) :+ weekdays(1) if schedules.contains(day) // Starting from Monday
      maybeOpenHours <- schedules.get(day)
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
