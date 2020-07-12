package exception

case class InvalidInputException(msg: String) extends Exception(msg)
