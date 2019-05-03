package gameplayLib


import gameplayLib.Phrase.Phrase
import gameplayLib.Position.Position
import gameplayLib.Who.Who


case class PlayerBuffNeed(needBuffEffect: BuffEffect, stack: Int) extends ExCondition

case class LifeBelow(lifeValue: Int) extends ExCondition


sealed trait ExCondition

object ExCondition{
  def conditionIsOk(exCondition: ExCondition,activePlayerStatus: OnePlayerStatus): Boolean = exCondition match {
    case PlayerBuffNeed(needBuffEffect, stack) => activePlayerStatus.buffs.map(x => x.buffEffect).count(y => y.getClass == needBuffEffect.getClass) >= stack
    case LifeBelow(lifeValue) => activePlayerStatus.HP < lifeValue
    case _ => false
  }
}

case class BaseCondition(phrase: Phrase, positions: Seq[Position], whoSTricks: Seq[Who])

case class CardSkill(baseCondition: BaseCondition, cond: Seq[ExCondition], effects: Seq[SkillEffect]) {
  def checkConditionToEffects: Seq[SkillEffect] = ???
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

sealed  trait SkillEffect
object SkillEffect{
  def activeSkillEffect(skillEffect: SkillEffect,gamePlayGround: GamePlayGround, caster: String, obj: String): GamePlayGround = ???
}