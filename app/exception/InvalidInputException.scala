package exception

case class InvalidInputException(msg: String) extends RuntimeException(msg)
