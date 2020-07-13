package controllers


import java.util.Locale

import com.typesafe.scalalogging.StrictLogging
import javax.inject.{Inject, Singleton}
import models.WeeklySchedule
import play.api.libs.json.JsValue
import play.api.mvc._
import services.{DateTimeAndLocaleService, WeekScheduleFormatter}

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

@Singleton
class OpeningHoursController @Inject()(cc: ControllerComponents,
                                       formatter: WeekScheduleFormatter,
                                       dateTimeAndLocaleService: DateTimeAndLocaleService)(implicit ec: ExecutionContext)
  extends AbstractController(cc) with StrictLogging {

  implicit val locale: Locale = dateTimeAndLocaleService.getLocale

  def convertToHumanReadableText: Action[JsValue] = Action(parse.json) { request =>
    request.body.validate[WeeklySchedule].fold(error => BadRequest("Input could not be parsed : " + error), {
      schedule =>
        val openDurationsInHumanText = Try {
          formatter.formatToHuman(schedule)
        }

        openDurationsInHumanText match {
          case Success(humanReadable) =>
            logger.debug("Successfully transformed to human readable text ")
            Ok(humanReadable)
          case Failure(exception) =>
            logger.error("Exception occurred : " + exception)
            BadRequest(exception.getMessage)
        }

    })

  }
}
