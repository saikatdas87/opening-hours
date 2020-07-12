package controllers


import com.typesafe.scalalogging.StrictLogging
import javax.inject.{Inject, Singleton}
import models.WeeklySchedule
import play.api.libs.json.JsValue
import play.api.mvc._
import services.WeekScheduleFormatter

import scala.concurrent.ExecutionContext

@Singleton
class OpeningHoursController @Inject()(cc: ControllerComponents, formatter: WeekScheduleFormatter)(implicit ec: ExecutionContext)
  extends AbstractController(cc) with StrictLogging {

  def convertToHumanReadableText: Action[JsValue] = Action(parse.json) { request =>
    request.body.validate[WeeklySchedule].fold(error => BadRequest("Input could not be parsed : " + error), {
      schedule =>
        logger.debug("Input : " + formatter.formatToHuman(schedule))
        Ok("Your new application is ready.")
    })

  }
}
