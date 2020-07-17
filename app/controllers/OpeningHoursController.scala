package controllers


import java.util.Locale

import com.typesafe.scalalogging.StrictLogging
import javax.inject.{Inject, Singleton}
import models.WeeklySchedule
import play.api.libs.json.JsValue
import play.api.mvc._
import services.{DateTimeAndLocaleService, RawScheduleToHumanFormatter, WeekScheduleFormatter}
import validations.OpenHoursScheduleValidator

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

@Singleton
class OpeningHoursController @Inject()(cc: ControllerComponents,
                                       rawDataFormatter: WeekScheduleFormatter,
                                       rawDataToHumanFormatter: RawScheduleToHumanFormatter,
                                       validator: OpenHoursScheduleValidator,
                                       dateTimeAndLocaleService: DateTimeAndLocaleService)(implicit ec: ExecutionContext)
  extends AbstractController(cc) with StrictLogging {


  def convertToHumanReadableText: Action[JsValue] = Action(parse.tolerantJson) { request =>
    request.body.validate[WeeklySchedule].fold(error => BadRequest("Invalid input, could not be parsed : " + error), {
      schedule =>
        implicit val locale: Locale = dateTimeAndLocaleService.getLocale
        Try {
          // Validate if all times are within the range of 0 to 86399
          validator.validateTime(schedule)
          // Validation each day ranges are proper :
          // closing time after open
          schedule.sortedDays.flatten.foreach(validator.validateTimesAreInOrder)
          val rawScheduleMap = rawDataFormatter.processRangeToRawScheduleMap(schedule)
          // Validate if we have some 2 consecutive open/close or missing close
          // Although we have already checked it and wrapped the failures in Failure(exp) when processing
          validator.validateParsedRawData(rawScheduleMap.values)
          val validRawScheduleMap = rawScheduleMap.mapValues(_.map {
            _.collect { case Success(range) => range }
          })

          rawDataToHumanFormatter.rawScheduleToHumanText(validRawScheduleMap)
        } match {
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
