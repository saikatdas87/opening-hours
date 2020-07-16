package integration

import java.io.InputStream

import models.{HoursSchedule, WeeklySchedule}
import play.api.libs.json.{JsValue, Json}
import play.api.test._

class OpeningHourAPISpec extends PlaySpecification {
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


  "The Opening Hour api" should {

    "deliver a formatted schedule in human readable form" in new WithApplication {
      val Some(result) = route(app, FakeRequest(PUT, "/opening-hours/toHuman").withBody(Json.toJson(schedule)))
      val content: String = contentAsString(result)

      status(result) must equalTo(OK)
      content must equalTo(successStr)
      contentType(result) must beSome("text/plain")
    }

    "deliver human readable text with valid_input_1.json" in new WithApplication {
      val inputStream: InputStream = getClass.getResourceAsStream("/resources/valid_input_1.json")
      val json: JsValue = try {
        Json.parse(inputStream)
      } finally {
        inputStream.close()
      }

      val Some(result) = route(app, FakeRequest(PUT, "/opening-hours/toHuman").withBody(json))
      val content: String = contentAsString(result)

      status(result) must equalTo(OK)
      content must equalTo(successStr)
      contentType(result) must beSome("text/plain")

    }

    "deliver human readable text with valid_input_2.json" in new WithApplication {
      val inputStream: InputStream = getClass.getResourceAsStream("/resources/valid_input_2.json")
      val json: JsValue = try {
        Json.parse(inputStream)
      } finally {
        inputStream.close()
      }
      val expected: String = """Monday: Closed
                       |Tuesday: Closed
                       |Wednesday: Closed
                       |Thursday: Closed
                       |Friday: 6 PM - 1 AM
                       |Saturday: 9 AM - 11 AM, 4 PM - 11 PM
                       |Sunday: Closed""".stripMargin
      val Some(result) = route(app, FakeRequest(PUT, "/opening-hours/toHuman").withBody(json))
      val content: String = contentAsString(result)

      status(result) must equalTo(OK)
      content must equalTo(expected)
      contentType(result) must beSome("text/plain")

    }

    "fails and returns bad request with invalid input invalid_no_close.json" in new WithApplication {
      val inputStream: InputStream = getClass.getResourceAsStream("/resources/invalid_no_close.json")
      val json: JsValue = try {
        Json.parse(inputStream)
      } finally {
        inputStream.close()
      }
      val Some(result) = route(app, FakeRequest(PUT, "/opening-hours/toHuman").withBody(json))
      val content: String = contentAsString(result)

      status(result) must equalTo(BAD_REQUEST)
      contentType(result) must beSome("text/plain")
      content must equalTo("One or more range pairs (type or value) in input are invalid")
    }

    "fails and returns bad request with outside integer invalid_time.json" in new WithApplication {
      val inputStream: InputStream = getClass.getResourceAsStream("/resources/invalid_time.json")
      val json: JsValue = try {
        Json.parse(inputStream)
      } finally {
        inputStream.close()
      }
      val Some(result) = route(app, FakeRequest(PUT, "/opening-hours/toHuman").withBody(json))
      val content: String = contentAsString(result)

      status(result) must equalTo(BAD_REQUEST)
      contentType(result) must beSome("text/plain")
      content must contain("Invalid input, could not be parsed :")
    }

    "fails and returns bad request with time outside range 0 to 86399 in invalid_time_outside_range.json" in new WithApplication {
      val inputStream: InputStream = getClass.getResourceAsStream("/resources/invalid_time_outside_range.json")
      val json: JsValue = try {
        Json.parse(inputStream)
      } finally {
        inputStream.close()
      }
      val Some(result) = route(app, FakeRequest(PUT, "/opening-hours/toHuman").withBody(json))
      val content: String = contentAsString(result)

      status(result) must equalTo(BAD_REQUEST)
      contentType(result) must beSome("text/plain")
      content must equalTo("Time 86400 outside valid Range 0 to 86399")
    }



  }
}
