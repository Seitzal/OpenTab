package eu.seitzal.opentab.models

import eu.seitzal.opentab._
import shortcuts._
import upickle.{default => json}

import play.api.db.Database

import scala.collection.mutable.ArrayBuffer
import scala.util.Random

object RandomPairings {

  def apply(tab: Tab)(implicit db: Database) : Draw = {
    val random = new Random
    val pool = random.shuffle(tab.teams.filter(_.active)).toBuffer
    val acc = new ArrayBuffer[(String, String)]

    // First pass: Avoid delegation matches, rematches, side imbalance
    def rule1(i : Int) : Boolean = 
      pool(0).previousOpponents.contains(pool(i).name) ||
      pool(0).delegation == pool(i).delegation ||
      pool(0).sideTendency * pool(i).sideTendency > 0

    // Second pass: Avoid delegation matches, rematches
    def rule2(i : Int) : Boolean = 
      pool(0).previousOpponents.contains(pool(i).name) ||
      pool(0).delegation == pool(i).delegation

    // Third pass: Avoid delegation matches
    def rule3(i : Int) : Boolean = 
      pool(0).delegation == pool(i).delegation

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