package controllers

import java.util.Locale

import exception.InvalidInputException
import models.{HoursSchedule, WeeklySchedule}
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.libs.json.Json
import play.api.test.Helpers.{contentAsString, stubControllerComponents, _}
import play.api.test.{FakeRequest, Injecting}
import services.{DateTimeAndLocaleService, WeekScheduleFormatter}

import scala.concurrent.ExecutionContextExecutor

class OpeningHoursControllerSpec extends PlaySpec with GuiceOneAppPerTest with Injecting with MockitoSugar {

  private val formatter: WeekScheduleFormatter = mock[WeekScheduleFormatter]
  private val dateTimeAndLocaleService: DateTimeAndLocaleService = mock[DateTimeAndLocaleService]
  private implicit val executor: ExecutionContextExecutor = scala.concurrent.ExecutionContext.global
  implicit val locale: Locale = Locale.ENGLISH
  private val controller = new OpeningHoursController(stubControllerComponents(), formatter, dateTimeAndLocaleService)
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

  "OpeningHoursController" should {
    when(dateTimeAndLocaleService.getLocale) thenReturn locale
    "return success and return formatted test" in {
      when(formatter.formatToHuman(schedule)) thenReturn successStr
      val res = controller.convertToHumanReadableText()(FakeRequest().withBody(Json.toJson(schedule)))

      contentType(res) must be(Some("text/plain"))
      contentAsString(res) must equal(successStr)
      status(res) must equal(OK)
    }

    "return success is only monday is passed" in {
      val inputStream = getClass.getResourceAsStream("/resources/valid_input_1.json")
      val json = try {
        Json.parse(inputStream)
      } finally {
        inputStream.close()
      }
      when(formatter.formatToHuman(schedule)) thenReturn successStr
      val res = controller.convertToHumanReadableText()(FakeRequest().withBody(json))

      contentType(res) must be(Some("text/plain"))
      contentAsString(res) must equal(successStr)
      status(res) must equal(OK)
    }

    "returns BadRequest in case of any Exception" in {
      when(formatter.formatToHuman(schedule)) thenAnswer (_ => InvalidInputException("Some exception"))
      val res = controller.convertToHumanReadableText()(FakeRequest().withBody(Json.toJson(schedule)))

      status(res) must equal(BAD_REQUEST)
    }

    "returns BadRequest in case of any runtime Exception" in {
      when(formatter.formatToHuman(schedule)) thenAnswer (_ => new RuntimeException("Some exception"))
      val res = controller.convertToHumanReadableText()(FakeRequest().withBody(Json.toJson(schedule)))

      status(res) must equal(BAD_REQUEST)
    }

    "Returns BadRequest if input is not parsable " in {
      val res = controller.convertToHumanReadableText()(FakeRequest().withBody(Json.toJson("""{"rowsDeleted":1}""")))

      status(res) must equal(BAD_REQUEST)
    }

    "Returns BadRequest if type is invalid " in {
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
