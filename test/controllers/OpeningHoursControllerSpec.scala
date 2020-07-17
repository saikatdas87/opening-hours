package controllers

import java.util.Locale

import exception.InvalidInputException
import models.{HoursSchedule, OpenDuration, WeeklySchedule}
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.libs.json.Json
import play.api.test.Helpers.{contentAsString, stubControllerComponents, _}
import play.api.test.{FakeRequest, Injecting}
import services.{DateTimeAndLocaleService, RawScheduleToHumanFormatter, WeekScheduleFormatter}
import validations.OpenHoursScheduleValidator

import scala.concurrent.ExecutionContextExecutor
import scala.util.Success

class OpeningHoursControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting with MockitoSugar {

  private val rawDataFormatter: WeekScheduleFormatter = mock[WeekScheduleFormatter]
  private val humanTextFormatter: RawScheduleToHumanFormatter = mock[RawScheduleToHumanFormatter]
  private val dateTimeAndLocaleService: DateTimeAndLocaleService = mock[DateTimeAndLocaleService]
  private val validator: OpenHoursScheduleValidator = mock[OpenHoursScheduleValidator]
  private implicit val executor: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
  implicit val locale: Locale = Locale.ENGLISH
  private val controller = new OpeningHoursController(stubControllerComponents(), rawDataFormatter, humanTextFormatter, validator, dateTimeAndLocaleService)
  private val schedule =
    WeeklySchedule(monday = Some(Seq(HoursSchedule("open", 3600), HoursSchedule("close", 18000))),
      tuesday = None,
      wednesday = None,
      thursday = None,
      friday = None,
      saturday = None,
      sunday = None)
  private val successStr =
    """Monday: 1 AM - 5 AM
      |Tuesday: Closed
      |Wednesday: Closed
      |Thursday: Closed
      |Friday: Closed
      |Saturday: Closed
      |Sunday: Closed""".stripMargin
  private val rawScheduleMap = Map(
    "Monday" -> Some(Seq(Success(OpenDuration(3600, 18000)))),
    "Tuesday" -> None,
    "Wednesday" -> None,
    "Thursday" -> None,
    "Friday" -> None,
    "Saturday" -> None,
    "Sunday" -> None
  )


  "convertToHumanReadableText()" should {
    when(dateTimeAndLocaleService.getLocale) thenReturn locale

    "returns success and return formatted test" in {
      doNothing().when(validator).validateTime(schedule)
      doNothing().when(validator).validateTimesAreInOrder(any[Seq[HoursSchedule]])
      doNothing().when(validator).validateTimesAreInOrder(any[Seq[HoursSchedule]])
      when(rawDataFormatter.processRangeToRawScheduleMap(schedule)) thenReturn rawScheduleMap

      when(humanTextFormatter.rawScheduleToHumanText(rawScheduleMap.mapValues(_.map {
        _.collect { case Success(range) => range }
      }))) thenReturn successStr
      val res = controller.convertToHumanReadableText()(FakeRequest().withBody(Json.toJson(schedule)))

      contentType(res) must be(Some("text/plain"))
      contentAsString(res) must equal(successStr)
      status(res) must equal(OK)
    }

    "returns success is only monday is passed" in {
      val inputStream = getClass.getResourceAsStream("/resources/valid_input_1.json")
      val json = try {
        Json.parse(inputStream)
      } finally {
        inputStream.close()
      }
      doNothing().when(validator).validateTime(schedule)
      doNothing().when(validator).validateTimesAreInOrder(any[Seq[HoursSchedule]])
      doNothing().when(validator).validateTimesAreInOrder(any[Seq[HoursSchedule]])
      when(rawDataFormatter.processRangeToRawScheduleMap(schedule)) thenReturn rawScheduleMap
      val res = controller.convertToHumanReadableText()(FakeRequest().withBody(json))

      contentType(res) must be(Some("text/plain"))
      contentAsString(res) must equal(successStr)
      status(res) must equal(OK)
    }

    "returns BadRequest in any of the validation fails" in {
      when(validator.validateTime(any())) thenThrow InvalidInputException("Outside range")
      val res = controller.convertToHumanReadableText()(FakeRequest().withBody(Json.toJson(schedule)))

      status(res) must equal(BAD_REQUEST)
    }

    "returns BadRequest if case of any Exception" in {
      when(rawDataFormatter.processRangeToRawScheduleMap(schedule)) thenAnswer (_ => InvalidInputException("Some exception"))
      val res = controller.convertToHumanReadableText()(FakeRequest().withBody(Json.toJson(schedule)))

      status(res) must equal(BAD_REQUEST)
    }

    "returns BadRequest if input is not parsable " in {
      val res = controller.convertToHumanReadableText()(FakeRequest().withBody(Json.toJson("""{"rowsDeleted":1}""")))

      status(res) must equal(BAD_REQUEST)
    }

    "returns BadRequest if type is invalid " in {
      val inputStream = getClass.getResourceAsStream("/resources/invalid-req.json")
      val json = try {
        Json.parse(inputStream)
      } finally {
        inputStream.close()
      }
      val res = controller.convertToHumanReadableText()(FakeRequest().withBody(json))

      status(res) must equal(BAD_REQUEST)
    }

  }
}
