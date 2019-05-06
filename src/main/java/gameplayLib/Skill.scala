package gameplayLib


import gameplayLib.Phrase.Phrase
import gameplayLib.Position.Position
import gameplayLib.Who.Who


case class PlayerBuffNeed(needBuffEffect: BuffEffect, stack: Int) extends CasterNeedCondition

case class LifeBelow(lifeValue: Int) extends CasterNeedCondition


sealed trait CasterNeedCondition

object CasterNeedCondition {
  def casterConditionIsOk(condition: CasterNeedCondition, activePlayerStatus: OnePlayerStatus): Boolean = condition match {
    case PlayerBuffNeed(needBuffEffect, stack) => activePlayerStatus.buffs.map(x => x.buffEffect).count(y => y.getClass == needBuffEffect.getClass) >= stack
    case LifeBelow(lifeValue) => activePlayerStatus.HP < lifeValue
    case _ => false
  }
}

case class BaseNeedCondition(phrase: Phrase, positions: Seq[Position])


case class CardSkill(baseCondition: BaseNeedCondition, cond: Seq[CasterNeedCondition], effects: Seq[SkillEffect]) {
  def checkAllConditionIsOkThenGetEffect(phrase: Phrase, position: Position, onePlayerStatus: OnePlayerStatus): Seq[SkillEffect] = {
    if (baseCondition.phrase == phrase
      && baseCondition.positions.contains(position)
      && cond.forall(aCasterNeedCond => CasterNeedCondition.casterConditionIsOk(aCasterNeedCond, onePlayerStatus))) {
      effects
    }
    else Nil
  }
}

case class AddBuffToPlayer(toWho: Who, buff: Buff) extends SkillEffect

case class DelBuffFromPlayer(toWho: Who, buff: Buff) extends SkillEffect

case class DoDamageToPlayer(toWho: Who, damSeq: Seq[Int]) extends SkillEffect

case class DirectKillPlayer(toWho: Who) extends SkillEffect

case class AddBuffToCard(toWho: Who, where: Position, buff: Buff) extends SkillEffect

case class DelBuffToCard(toWho: Who, where: Position, buff: Buff) extends SkillEffect

case class AddCard(toWho: Who, where: Position, Cards: Seq[Card]) extends SkillEffect

case class DropMinCard(toWho: Who, Num: Int) extends SkillEffect

case class DropMaxCard(toWho: Who, Num: Int) extends SkillEffect

case class DrawCard(toWho: Who, Num: Int) extends SkillEffect

case class GiveCard(FromWho: Who, toWho: Who, fromMin: Boolean, Num: Int) extends SkillEffect

case class DestroyCertainCard(toWho: Who, wheres: Seq[Position], cardId: Int) extends SkillEffect

case class DestroyMaxCard(toWho: Who, where: Seq[Position], Num: Int) extends SkillEffect

sealed trait SkillEffect

object SkillEffect {
  def activeSkillEffectToGamePlayGroundAndSpawnCards(casterToSeqEffect: Map[(String, Option[String]), Seq[SkillEffect]], gamePlayGround: GamePlayGroundValuesWhatSkillEffectCanChange, opponent:Option[String],spawningCard: Seq[Card]): (GamePlayGroundValuesWhatSkillEffectCanChange,Seq[Card]) = {



    (gamePlayGround,spawningCard)
  }

  def activeSkillEffectToFormCard(casterToSeqEffect: Map[(String, Option[String]), Seq[SkillEffect]], thisShapeNeedCounter: Option[Shape], opShapeNeedCounter: Option[Shape], formCards: Seq[Card]): (Seq[Option[Shape]], Seq[Option[Shape]], Seq[Card]) = ???

}