package controllers


import com.typesafe.scalalogging.StrictLogging
import javax.inject.{Inject, Singleton}
import models.WeeklySchedule
import play.api.libs.json.JsValue
import play.api.mvc._

import scala.concurrent.ExecutionContext

@Singleton
class OpeningHoursController @Inject()(cc: ControllerComponents)(implicit ec: ExecutionContext)
  extends AbstractController(cc) with StrictLogging {

  def convertToHumanReadableText: Action[JsValue] = Action(parse.json) { request =>
    request.body.validate[WeeklySchedule].fold(error => BadRequest("ss" + error), {
      schedule =>
        logger.debug("Input : " + schedule.monday)
        Ok("Your new application is ready.")
    })

  }
}
