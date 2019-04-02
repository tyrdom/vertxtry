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

  def genPointMapAndSpecial(Cards: Seq[Card]): (Seq[(Int, Int)], Int, Int) = {
    val map: Seq[(Int, Int)] = (Config.minPoint to Config.maxPoint).foldLeft(Nil: Seq[(Int, Int)])((m, i) => (i, Cards.count(c => c.standard.nowPoint == i)) +: m)

    val dCard = Config.minPoint - 1
    val xCard = Config.maxPoint + 1

    (map, Cards.count(x => x.standard.nowPoint <= dCard), Cards.count(x => x.standard.nowPoint >= xCard))
  }

  def compareCardLessThan(a: Card, b: Card): Boolean = a.standard.nowPoint < b.standard.nowPoint || (a.standard.nowPoint == b.standard.nowPoint && a.standard.id == b.standard.id)

  def genMaxShapes(cards: Seq[Card]): Shape = {
    val (pointSeq, d, x) = genPointMapAndSpecial(cards)

    val sortedSeq = pointSeq.sortBy(p => (p._2, p._1))(Ordering.Tuple2(Ordering.Int.reverse, Ordering.Int.reverse))
    val maxHeight = sortedSeq.head._2
    val maxHeightPoint = sortedSeq.head._1
    var shapeTemp = (maxHeightPoint, maxHeight + x, maxHeight + x, 1) //口袋计算最大可组的牌型=（点数，面积（牌数），高度，宽度）


    Shape(1, 1, 1, 1, 1)
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