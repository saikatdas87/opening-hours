package integration

import models.{HoursSchedule, WeeklySchedule}
import play.api.libs.json.Json
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

  }
}
