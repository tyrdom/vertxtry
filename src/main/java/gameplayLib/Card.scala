package gameplayLib

import gameplayLib.Type.SpawnType
import gameplayLib.Trick.Trick


object Trick extends Enumeration {
  type Trick = Value
  val Hold = Value
  val CastWith = Value
  val AnyCast = Value
  val ThisCast = Value
  val OtherCast = Value
}

object Type extends Enumeration {
  type SpawnType = Value
  val Straight = Value
  val Multi = Value
  val NoShape = Value
  val Single = Value
}


class CardEffect {


}

case class CommonValue(id: Int, level: Int, nowPoint: Int, copy: Boolean)

case class Skill(tricks: Seq[Trick], effects: Seq[CardEffect])

case class Shape(keyPoint: Int, height: Int, length: Int, extraNum: Int, fillBlankRestNum: Int)

case class Card(standard: CommonValue, owner: Option[String], skill: Option[Skill]) {


  def sortCard(Cards: Seq[Card]): Seq[Card] = Cards.sortWith(compareCardLessThan)

  def genPointMap(Cards: Seq[Card]): Map[Int, Int] = {
    var map: Map[Int, Int] = Map()
    val min = Config.minPoint - 1
    val max = Config.maxPoint + 1
    for (i <- min to max) {
      if (i == min) {
        map += (min -> Cards.count(x => x.standard.nowPoint <= min))
      }
      else if (i == max) {
        map += (max -> Cards.count(x => x.standard.nowPoint >= max))
      }
      else

        map += (max -> Cards.count(x => x.standard.nowPoint == i))

    }
    map
  }

  def compareCardLessThan(a: Card, b: Card): Boolean = a.standard.nowPoint < b.standard.nowPoint || (a.standard.nowPoint == b.standard.nowPoint && a.standard.id == b.standard.id)

  def genShapes(cards: Seq[Card]): Seq[Shape] = {
    val pointMap = genPointMap(cards)

    Shape(1, 1, 1, 1, 1) +: Nil
  }

  def canShapeCounter(cards: Seq[Card], shape: Shape): Option[Shape] = ???

  //  def genMultiS(key: Int, usePoint: Seq[Int], temp: Seq[Shape]): Seq[Shape] = key match {
  //    case Config.maxPoint => Shape(Type.Multi, Config.maxPoint, usePoint.count(_ == Config.maxPoint), usePoint.count(_ != Config.maxPoint), 0) +: temp
  //    case nowKey =>
  //      val nextTemp = Shape(Type.Multi, nowKey, usePoint.count(_ == nowKey), usePoint.count(_ != nowKey), 0) +: temp
  //      genMultiS(key + 1, usePoint, nextTemp)
  //  }


  def genStraightS(usePoints: Seq[Int], temp: Seq[Shape], fillBlankNum: Int): Seq[Shape] = ???

  def genAStraight(nowPoint: Int, restPoints: Seq[Int], fillBlank: Int, nowShape: Shape): Shape = {
    def straightNextPoint(x: Int) = x match {
      case Config.maxPoint => Config.minPoint
      case xx => xx + 1
    }

    val nextPoint = straightNextPoint(nowPoint)

  }

}