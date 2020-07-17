package services

import java.text.DateFormatSymbols
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

import com.google.inject.{ImplementedBy, Inject}
import com.typesafe.config.Config
import com.typesafe.scalalogging.StrictLogging
import play.api.Configuration

@ImplementedBy(classOf[DateTimeAndLocaleServiceImpl])
trait DateTimeAndLocaleService {
  def getLocale: Locale

  def getLocaleWeekDays(implicit locale: Locale): Array[String]

  def unixTimeToHuman(unixTime: Int)(implicit locale: Locale): String
}

class DateTimeAndLocaleServiceImpl @Inject()(configuration: Configuration) extends DateTimeAndLocaleService with StrictLogging {

  private lazy val applicationConfig: Config = configuration.underlying
  private val configLocaleKey: String = "locale"
  private val configDateDisplayFormatKey = "display.date.format"
  private lazy val defaultDisplayDateFormat = "hh:mm:ss a"
  private lazy val defaultLocale: Locale = Locale.ENGLISH

  def getLocale: Locale = {
    try {
      val configLocale = applicationConfig.getString(configLocaleKey)
      if (configLocale != null && configLocale.nonEmpty) new Locale(configLocale) else defaultLocale
    } catch {
      case _: Exception =>
        logger.info(s"No Locale config found. Using default locale {$defaultLocale} for formatting")
        defaultLocale
    }
  }

  def getLocaleWeekDays(implicit locale: Locale): Array[String] = {
    new DateFormatSymbols(useLocale).getWeekdays
  }

  def unixTimeToHuman(unixTime: Int)(implicit locale: Locale): String = {
    val configFormat = applicationConfig.getString(configDateDisplayFormatKey)
    val format = if (configFormat != null && configFormat.nonEmpty) configFormat else defaultDisplayDateFormat

    val dateFormatter = DateTimeFormatter.ofPattern(format, useLocale)
    dateFormatter.format(LocalTime.ofSecondOfDay(unixTime))
      .replace(":00:00", "") // Replace zero minutes, seconds
      .replace(":00 ", " ") // Replace zero seconds
      .replaceFirst("^0*", "") // Replace leading zeros
  }

  private def useLocale(implicit locale: Locale): Locale = if (null != locale) locale else Locale.ENGLISH
}
