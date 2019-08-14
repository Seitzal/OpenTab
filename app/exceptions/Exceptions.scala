package eu.seitzal.opentab.exceptions

final class UserNotFoundException(username: String) 
    extends Exception("No user named \"" + username + "\" found in database.")