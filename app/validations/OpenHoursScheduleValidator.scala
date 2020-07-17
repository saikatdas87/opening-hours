package validations

import com.google.inject.ImplementedBy
import exception.InvalidInputException
import models.{HoursSchedule, OpenDuration, WeeklySchedule}

import scala.util.Try

@ImplementedBy(classOf[OpenHoursScheduleValidatorImpl])
trait OpenHoursScheduleValidator {
  def validateTime(rawInput: WeeklySchedule): Unit

  def validateTimesAreInOrder(ranges: Seq[HoursSchedule]): Unit

  def validateParsedRawData(rawParsed: Iterable[Option[Seq[Try[OpenDuration]]]]): Unit
}

class OpenHoursScheduleValidatorImpl extends OpenHoursScheduleValidator {

  private val validTimeRange = 0 to 86399

  private def isSorted[T](s: Seq[T])(implicit ord: Ordering[T]): Boolean = s match {
    case Seq() => true
    case Seq(_) => true
    case _ => s.sliding(2).forall { case Seq(prev, next) => ord.lt(prev, next) }
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
    if (!isSorted(times)) throw InvalidInputException("One (or maybe more) opening closing hours are not in order for a one or more day(s)")
  }


  def validateParsedRawData(rawParsed: Iterable[Option[Seq[Try[OpenDuration]]]]): Unit = {
    if (!rawParsed.flatten.flatten.forall(_.isSuccess))
      throw InvalidInputException("One (or maybe more) range pairs(close & open) are invalid")
  }

}
