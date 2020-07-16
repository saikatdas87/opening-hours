package services

import java.text.DateFormatSymbols
import java.util.Locale

import com.typesafe.config.Config
import org.junit.Assert.assertTrue
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Configuration
import play.api.test.Injecting

class DateTimeAndLocaleServiceSpec extends PlaySpec with GuiceOneAppPerTest with Injecting with MockitoSugar {

  private val configuration: Configuration = mock[Configuration]
  private val config: Config = mock[Config]
  private val service = new DateTimeAndLocaleServiceImpl(configuration)

  "getLocale()" should {
    when(configuration.underlying) thenReturn config

    "return the locale configured in config file" in {
      when(config.getString("locale")) thenReturn "fi"
      val response = service.getLocale

      response must be(new Locale("fi"))
    }

    "return default if the locale configured empty or null" in {
      when(config.getString("locale")) thenReturn null
      val response = service.getLocale

      response must be(Locale.ENGLISH)
    }


    "return default if no config for locale" in {
      when(configuration.underlying) thenAnswer (_ => new RuntimeException("Some exception"))
      val response = service.getLocale

      response must be(Locale.ENGLISH)
    }

  }

  "getLocaleWeekDays()" should {

    "Returns weekdays based on locale" in {
      implicit val locale: Locale = new Locale("fi")

      val res = service.getLocaleWeekDays

      res must be(new DateFormatSymbols(locale).getWeekdays)
    }

    "Returns weekdays in English if based locale is null" in {
      implicit val locale: Locale = null

      val res = service.getLocaleWeekDays

      res must be(new DateFormatSymbols().getWeekdays)
    }

  }

  "unixTimeToHuman()" should {

    "Return time in human form" in {
      implicit val locale: Locale = Locale.ENGLISH

      val res = service.unixTimeToHuman(86399)

      res must be("11:59:59 PM")
    }

    "Replaces zero minutes and seconds" in {
      implicit val locale: Locale = Locale.ENGLISH

      val res = service.unixTimeToHuman(36000)
      res must be("10 AM")
    }

    "Replaces zero seconds" in {
      implicit val locale: Locale = Locale.ENGLISH

      val res = service.unixTimeToHuman(36060)
      res must be("10:01 AM")
    }

    "Keeps zero minutes" in {
      implicit val locale: Locale = Locale.ENGLISH

      val res = service.unixTimeToHuman(36010)
      res must be("10:00:10 AM")
    }

    "Replaces leading zero with Finnish locale" in {
      implicit val locale: Locale = new Locale("fi")

      val res = service.unixTimeToHuman(3600)
      res must be("1 ap.")
    }


    "Throws exception if unixTime outside 0 and 86399" in {
      implicit val locale: Locale = Locale.ENGLISH

      try {
        service.unixTimeToHuman(86399)
        fail("Should not have reached here")
      } catch {
        case _: Exception => assertTrue(true)
      }
    }

  }


}
