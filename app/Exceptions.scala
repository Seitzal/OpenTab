package eu.seitzal.opentab

final case class NotFoundException(
  what: String,
  propertyName: String,
  propertyValue: String) 
  extends Exception(
    "No " + what + " with " + propertyName + " \"" + propertyValue + 
      "\" exists")

final case class ExistsAlreadyException(
  what: String,
  propertyName: String,
  propertyValue: String)
  extends Exception(
    "There is already a " + what + " with " + propertyName + " \"" + 
      propertyValue + "\"")
