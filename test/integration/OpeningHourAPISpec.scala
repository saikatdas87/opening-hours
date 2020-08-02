package integration

import models.{HoursSchedule, WeeklySchedule}
import org.scalatest.Matchers.fail
import play.api.libs.json.Json
import play.api.test._

import scala.io.{BufferedSource, Source}
import scala.util.{Failure, Success, Try}

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
      Try {
        val source: BufferedSource = Source.fromFile("test/resources/valid_input_1.json")
        Json.parse(source.mkString)
      } match {
        case Success(json) =>
          val Some(result) = route(app, FakeRequest(PUT, "/opening-hours/toHuman").withBody(json))
          val content: String = contentAsString(result)

          status(result) must equalTo(OK)
          content must equalTo(successStr)
          contentType(result) must beSome("text/plain")
        case Failure(exception) =>
          fail("Should not have failed with : " + exception)
      }
    }

    "deliver human readable text with valid_input_2.json" in new WithApplication {
      Try {
        val source: BufferedSource = Source.fromFile("test/resources/valid_input_2.json")
        Json.parse(source.mkString)
      } match {
        case Success(json) =>
          val expected: String =
            """Monday: Closed
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
        case Failure(exception) =>
          fail("Should not have failed with : " + exception)
      }
    }

    "fails and returns bad request with invalid input invalid_no_close.json" in new WithApplication {
      Try {
        val source: BufferedSource = Source.fromFile("test/resources/invalid_no_close.json")
        Json.parse(source.mkString)
      } match {
        case Success(json) =>
          val Some(result) = route(app, FakeRequest(PUT, "/opening-hours/toHuman").withBody(json))
          val content: String = contentAsString(result)

          status(result) must equalTo(BAD_REQUEST)
          contentType(result) must beSome("text/plain")
          content must equalTo("One (or maybe more) range pairs(close & open) are invalid")
        case Failure(exception) =>
          fail("Should not have failed with : " + exception)
      }
    }

    "fails and returns bad request with outside integer invalid_time.json" in new WithApplication {
      Try {
        val source: BufferedSource = Source.fromFile("test/resources/invalid_time.json")
        Json.parse(source.mkString)
      } match {
        case Success(json) =>
          val Some(result) = route(app, FakeRequest(PUT, "/opening-hours/toHuman").withBody(json))
          val content: String = contentAsString(result)

          status(result) must equalTo(BAD_REQUEST)
          contentType(result) must beSome("text/plain")
          content must contain("Invalid input, could not be parsed :")
        case Failure(exception) =>
          fail("Should not have failed with : " + exception)
      }
    }

    "fails and returns bad request with time outside range 0 to 86399 in invalid_time_outside_range.json" in new WithApplication {
      Try {
        val source: BufferedSource = Source.fromFile("test/resources/invalid_time_outside_range.json")
        Json.parse(source.mkString)
      } match {
        case Success(json) =>
          val Some(result) = route(app, FakeRequest(PUT, "/opening-hours/toHuman").withBody(json))
          val content: String = contentAsString(result)

          status(result) must equalTo(BAD_REQUEST)
          contentType(result) must beSome("text/plain")
          content must equalTo("Time 86400 outside valid Range 0 to 86399")
        case Failure(exception) =>
          fail("Should not have failed with : " + exception)
      }
    }

  }
}
