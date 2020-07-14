package validations

import com.google.inject.ImplementedBy
import exception.InvalidInputException
import models.{HoursSchedule, WeeklySchedule}

@ImplementedBy(classOf[OpenHoursScheduleValidatorImpl])
trait OpenHoursScheduleValidator {
  def validateTime(rawInput: WeeklySchedule): Unit

  def validateTimesAreInOrder(ranges: Seq[HoursSchedule]): Unit

  def validateRangePairs(ranges: Seq[HoursSchedule]): Unit
}

class OpenHoursScheduleValidatorImpl extends OpenHoursScheduleValidator {

  private val validTimeRange = 0 to 86399

  private def isClosingTimeAfterOpen(closingTime: Int, openingTime: Int): Boolean = closingTime > openingTime

  private def isSorted[T](s: Seq[T])(implicit ord: Ordering[T]): Boolean = s match {
    case Seq() => true
    case Seq(_) => true
    case _ => s.sliding(2).forall { case Seq(prev, next) => ord.lt(prev, next) }
  }

  def validateRangePairs(ranges: Seq[HoursSchedule]): Unit = {
    if (ranges.length % 2 != 0)
      throw InvalidInputException("one or more schedules are not in pair of open, close")

    ranges.grouped(2) foreach {
      case Seq(start, end) =>
        if (!isClosingTimeAfterOpen(end.value, start.value))
          throw InvalidInputException(s"Closing time is before for this pair : open=${start.value}, close=${end.value}")
    }
  }

  def validateTime(rawInput: WeeklySchedule): Unit = {
    for {
      rawDaily <- rawInput.sortedDays
      time <- rawDaily.getOrElse(Seq.empty)
    } yield {
      if (!validTimeRange.contains(time.value))
        throw InvalidInputException(s"Time ${time.value} outside valid $validTimeRange")
    }
  }

  def validateTimesAreInOrder(ranges: Seq[HoursSchedule]): Unit = {
    val times = ranges.map(_.value)
    if (!isSorted(times)) throw InvalidInputException("One (or maybe more) opening closing hours are not in order for a particular day")
  }


}
