package models

import play.api.libs.json.{Json, OFormat}

case class WeeklySchedule(
                           monday: Option[Seq[HoursSchedule]],
                           tuesday: Option[Seq[HoursSchedule]],
                           wednesday: Option[Seq[HoursSchedule]],
                           thursday: Option[Seq[HoursSchedule]],
                           friday: Option[Seq[HoursSchedule]],
                           saturday: Option[Seq[HoursSchedule]],
                           sunday: Option[Seq[HoursSchedule]]
                         ) {
  def sortedDays = Seq(monday, tuesday, wednesday, thursday, friday, saturday, sunday)
}

object WeeklySchedule {
  implicit val productFormat: OFormat[WeeklySchedule] = Json.format[WeeklySchedule]
}
