package eu.seitzal.opentab

final case class NotFoundException(
  what: String,
  propertyName: String,
  propertyValue: String) 
  extends Exception(
    "No " + what + "with " + propertyName + " \"" + propertyValue + 
      "\" exists in database.")
