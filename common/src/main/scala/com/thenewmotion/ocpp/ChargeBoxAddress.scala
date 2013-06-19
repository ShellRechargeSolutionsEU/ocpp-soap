package com.thenewmotion.ocpp

import soapenvelope12.Envelope
import scalax.{RichAny, StringOption}
import xml.Elem
import scala.util.Try
import org.apache.commons.validator.routines.UrlValidator

/**
 * @author Yaroslav Klymko
 */
object ChargeBoxAddress {
  val validator = UrlValidator.getInstance()

  def unapply(env: Envelope): Option[Uri] = {
    val headers = for {
      header <- env.Header.toSeq
      dataRecord <- header.any
      elem <- dataRecord.value.asInstanceOfOpt[Elem] if elem.namespace == WsaAddressing.Uri
    } yield elem

    def loop(xs: List[String]): Option[Uri] = xs match {
      case Nil => None
      case h :: t => StringOption(h) match {
        case Some(WsaAddressing.AnonymousUri) => loop(t)
        case Some(uri) if validator.isValid(uri) => Try(new Uri(uri)).toOption orElse loop(t)
        case _ => loop(t)
      }
    }

    loop(List(
      headers \\ "From" \ "Address" text,
      headers \\ "ReplyTo" \ "Address" text))
  }
}
