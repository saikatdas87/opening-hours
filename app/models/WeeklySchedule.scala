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

  /*def validName(i: Seq[HoursSchedule] => Unit) = i != null
  implicit val reads: Reads[WeeklySchedule] = (
    (JsPath \ "monday").readNullable[Seq[HoursSchedule]] and
      (JsPath \ "tuesday").readNullable[Seq[HoursSchedule]] and
      (JsPath \ "wednesday").readNullable[Seq[HoursSchedule]] and
      (JsPath \ "thursday").readNullable[Seq[HoursSchedule]] and
      (JsPath \ "friday").readNullable[Seq[HoursSchedule]] and
      (JsPath \ "saturday").readNullable[Seq[HoursSchedule]] and
      (JsPath \ "sunday").readNullable[Seq[HoursSchedule]]
    ) (WeeklySchedule.apply _)

  val citiesCheckConstraint: Constraint[WeeklySchedule] = Constraint("constraints.citiescheck")({
    registerForm =>
      // you have access to all the fields in the form here and can
      // write complex logic here
      if (registerForm.monday.isDefined) {
        Valid
      } else {
        Invalid(Seq(ValidationError("City must be selected")))
      }
  })*/
}
