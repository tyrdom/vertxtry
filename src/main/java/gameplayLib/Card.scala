package gameplayLib

import gameplayLib.SpawnType.SpawnType
import gameplayLib.Trick.Trick

trait Card {

}


object Trick extends Enumeration {
  type Trick = Value
  val Hold = Value
  val Cast = Value
  val ThisCast = Value
  val OtherCast = Value
}

object SpawnType extends Enumeration {
  type SpawnType = Value
  val Straight = Value
  val Multi = Value
  val NoShape = Value
  val Single = Value
}


class CardEffect {


}

case class CommonCard(id: Int, level: Int, nowPoint: Int, copy: Boolean) extends Card

case class SkillCard(id: Int, standard: CommonCard, owner: String, skills: Skill) extends Card

case class Skill(tricks: Seq[Trick], effects: Seq[CardEffect])

object Card {


  def genLinkTuple(card: Card): Option[(Int, Int, Int)] = card match {
    case commonCard: CommonCard => Some(commonCard.nowPoint - 1, commonCard.nowPoint, commonCard.nowPoint + 1)
    case skillCard: SkillCard => {
      val point = skillCard.standard.nowPoint
      Some(point - 1, point, point + 1)
    }
    case _ => None

  }

  def getPoint(card: Card): Option[Int] = card match {
    case commonCard: CommonCard => Some(commonCard.nowPoint)
    case skillCard: SkillCard => Some(skillCard.standard.nowPoint)
    case _ => None
  }


  def order(cards: Seq[Card]): Seq[Card] = ??? //TODO

  def shape(cards: Seq[Card]): (SpawnType, Int) = {


    (SpawnType.NoShape, 7)
  }
}