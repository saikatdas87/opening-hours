package models

import play.api.libs.json.{Json, OFormat}

case class HoursSchedule(
                          `type`: String,
                          value: Int
                        ) {

}

object HoursSchedule {
  implicit val hoursScheduleFormat: OFormat[HoursSchedule] = Json.format[HoursSchedule]
}

object Status extends Enumeration {
  val open: Value = Value
  val close: Value = Value
}
