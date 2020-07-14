package models

import play.api.libs.json.{Json, OFormat}
case class HoursSchedule(
                          `type`: String,
                          value: Int
                        ) {
  def isClosingTime: Boolean = `type` == Status.close.toString
  def isOpeningTime: Boolean = `type` == Status.open.toString
}

object HoursSchedule {
  implicit val hoursScheduleFormat: OFormat[HoursSchedule] = Json.format[HoursSchedule]
}

object Status extends Enumeration {
  val open: Value = Value
  val close: Value = Value
}

case class OpenDuration(
                       start: Int,
                       end: Int
                       )
