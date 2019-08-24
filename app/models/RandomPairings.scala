package eu.seitzal.opentab.models

import eu.seitzal.opentab._
import shortcuts._
import upickle.{default => json}

import play.api.db.Database

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

object RandomPairings {

  def apply(tab: Tab, ruleset: String = "drs")(implicit db: Database) : Draw = {
    val random = new Random
    val pool = random.shuffle(tab.teams.filter(_.active)).toBuffer
    val acc = new ArrayBuffer[(String, String)]

    val crit2  = List(ruleset.charAt('0'), ruleset.charAt('1'))
    val crit3 = ruleset.charAt('0')

    // First pass: Avoid all three criteria
    def rule1(i : Int) : Boolean = 
      pool(0).previousOpponents.contains(pool(i).name) ||
      pool(0).delegation == pool(i).delegation ||
      pool(0).sideTendency * pool(i).sideTendency > 0

    // Second pass: Avoid two criteria
    def rule2(i : Int) : Boolean =
      (crit2.contains('r') && pool(0).previousOpponents.contains(pool(i).name)) ||
      (crit2.contains('d') && pool(0).delegation == pool(i).delegation) ||
      (crit2.contains('s') && pool(0).sideTendency * pool(i).sideTendency > 0)

    // Third pass: Avoid one criterion
    def rule3(i : Int) : Boolean = 
      (crit3 == 'r' && pool(0).previousOpponents.contains(pool(i).name)) ||
      (crit3 == 'd' && pool(0).delegation == pool(i).delegation) ||
      (crit3 == 's' && pool(0).sideTendency * pool(i).sideTendency > 0)

    // Fourth Pass: Everything goes
    def rule4(i : Int) = false

    def findMatch(rule : Int => Boolean)(i : Int) : Option[(Team, Team)] = {
      if (i == pool.length)
        None
      else if (rule(i))
        findMatch(rule)(i + 1)
      else if (pool(0).sideTendency < 0) {
        val result = Some((pool(0), pool(i)))
        pool.remove(i)
        pool.remove(0)
        result
      } else {
        val result = Some((pool(i), pool(0)))
        pool.remove(i)
        pool.remove(0)
        result
      }
    }
    var bye = false
    def pass(finder : Int => Option[(Team, Team)])
        (buffer : Vector[(Team, Team)] = Vector())
        : Vector[(Team, Team)] = {
      if (pool.length == 0) {
        buffer
      } else if (pool.length == 1) {
        bye = true;
        buffer
      } else {
        val next = finder(1)
        next match {
          case None          => {
            buffer
          }
          case Some(pairing) => {
            pass(finder)(buffer :+ pairing)
          }
        }
      }
    }

    val pairings = {
      val firstPass = (pass(findMatch(rule1))()).toList
      if (pool.length > 1) {
        val secondPass = (pass(findMatch(rule2))()).toList
        if (pool.length > 1) {
          val thirdPass = (pass(findMatch(rule3))()).toList
          if (pool.length > 1) {
            val fourthPass = (pass(findMatch(rule4))()).toList
            firstPass ++ secondPass ++ thirdPass ++ fourthPass
          } else {
            firstPass ++ secondPass ++ thirdPass
          }
        } else {
          firstPass ++ secondPass
        }
      } else {
        firstPass
      }
    }

    if (bye)
      (pairings, Some(pool(0)))
    else
      (pairings, None)
  }

}