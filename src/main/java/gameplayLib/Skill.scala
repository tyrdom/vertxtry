package gameplayLib


import gameplayLib.CardStandType.CardStandType

import gameplayLib.Phrase.Phrase
import gameplayLib.Position.Position
import gameplayLib.SkillEffect.FormSkillResult
import gameplayLib.Who.{All, Opponent, Other, This, Who}

sealed trait CasterAndCardsNeedCondition {
  def casterAndCardsCondIsOk(cards: Seq[Card], activePlayerStatus: OnePlayerStatus): Boolean
}

case class PlayerBuffNeed(needBuffEffect: BuffEffect, stack: Int) extends CasterAndCardsNeedCondition {
  override def casterAndCardsCondIsOk(cards: Seq[Card], activePlayerStatus: OnePlayerStatus): Boolean = activePlayerStatus.buffs.map(x => x.buffEffect).count(y => y.getClass == needBuffEffect.getClass) >= stack
}

case class LifeBelow(lifeValue: Int) extends CasterAndCardsNeedCondition {
  override def casterAndCardsCondIsOk(cards: Seq[Card], activePlayerStatus: OnePlayerStatus): Boolean = activePlayerStatus.HP < lifeValue
}

case class NotLessThanShape(shape: Shape) extends CasterAndCardsNeedCondition {
  override def casterAndCardsCondIsOk(cards: Seq[Card], activePlayerStatus: OnePlayerStatus): Boolean = {
    val maybeShape: Option[Shape] = Card.genBiggestShape(cards, activePlayerStatus.buffs, 0)
    shape.notBiggerThan(maybeShape)
  }

}


case class BaseNeedCondition(phrase: Phrase, positions: Seq[Position])


case class CardSkill(baseCondition: BaseNeedCondition, cond: Seq[CasterAndCardsNeedCondition], effects: Seq[SkillEffect]) {
  def checkAllConditionIsOkThenGetEffect(phrase: Phrase, position: Position, cards: Seq[Card], onePlayerStatus: OnePlayerStatus): Seq[SkillEffect] = {
    if (baseCondition.phrase == phrase
      && baseCondition.positions.contains(position)
      && cond.forall(aCasterNeedCond => aCasterNeedCond.casterAndCardsCondIsOk(cards, onePlayerStatus))) {
      effects
    }
    else Nil
  }
}


//////////////////////SkillEffectToActive////////////////////
case class AddBuffToPlayer(toWho: Who, buff: Buff) extends SkillEffect {
  override def activeFormEffect(gid: Int, lastResult: FormSkillResult): FormSkillResult = lastResult

  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = {
    var oldPlayersStatus = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus
    val newPlayersStatus: Map[String, OnePlayerStatus] = toWho match {
      case This => {
        val status: OnePlayerStatus = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus(caster).addBuff(buff)
        oldPlayersStatus += caster -> status
        oldPlayersStatus
      }
      case Other => oldPlayersStatus.map(x => {
        val player = x._1
        val oldStatus = x._2
        val newStatus = if (player == caster) oldStatus else oldStatus.addBuff(buff)
        player -> newStatus
      })
      case Opponent => {
        val status: OnePlayerStatus = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus(obj.getOrElse(caster)).addBuff(buff)
        oldPlayersStatus += obj.getOrElse(caster) -> status
        oldPlayersStatus
      }
      case All => oldPlayersStatus.map(x => {
        val player = x._1
        val oldStatus = x._2
        val newStatus = oldStatus.addBuff(buff)
        player -> newStatus
      })
    }
    gamePlayGroundValuesThatSkillEffectCanChange.playersStatus = newPlayersStatus
    (gamePlayGroundValuesThatSkillEffectCanChange, spawningCard)
  }


}


case class DoDamageToPlayer(toWho: Who, damSeq: Seq[Int]) extends SkillEffect {
  override def activeFormEffect(gid: Int, lastResult: FormSkillResult): FormSkillResult = lastResult

  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = {
    val players = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus.keys
    val oldDamages: Seq[Damage] = gamePlayGroundValuesThatSkillEffectCanChange.nowTurnDamage
    val newComeDamages = toWho match {
      case This => Seq(Damage(caster, caster, damSeq, false))
      case Other => players.filter(x => x != caster).map(obj => Damage(caster, obj, damSeq, false)).toSeq
      case Opponent => Seq(Damage(caster, obj.getOrElse(caster), damSeq, false))
      case All => players.map(obj => Damage(caster, obj, damSeq, false)).toSeq
    }
    val newDamage = oldDamages ++ newComeDamages
    gamePlayGroundValuesThatSkillEffectCanChange.nowTurnDamage = newDamage
    (gamePlayGroundValuesThatSkillEffectCanChange, spawningCard)
  }
}

case class DirectKillPlayer(toWho: Who) extends SkillEffect {
  override def activeFormEffect(gid: Int, lastResult: FormSkillResult): FormSkillResult = lastResult

  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = {

    val killPlayers = SkillEffect.getTheNamesWhoToEffect(gamePlayGroundValuesThatSkillEffectCanChange, toWho, caster, obj)

    val oldStatus = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus
    killPlayers.foreach(aPlayer => {
      val newOneStatus = oldStatus(aPlayer).kill
      gamePlayGroundValuesThatSkillEffectCanChange.playersStatus += (aPlayer -> newOneStatus)
    })
    (gamePlayGroundValuesThatSkillEffectCanChange, spawningCard)
  }
}

case class AddBuffToCard(toWho: Who, wheres: Seq[Position], buff: Buff) extends SkillEffect {
  override def activeFormEffect(gid: Int, lastResult: FormSkillResult): FormSkillResult = lastResult

  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = {

    val addPlayers = SkillEffect.getTheNamesWhoToEffect(gamePlayGroundValuesThatSkillEffectCanChange, toWho, caster, obj)
    var newSpawningCard = spawningCard
    wheres.foreach(x => {
      val aWhere = x
      aWhere match {
        case gameplayLib.Position.DrawDeck => gamePlayGroundValuesThatSkillEffectCanChange.drawDeck = gamePlayGroundValuesThatSkillEffectCanChange.drawDeck.map(_.addBuff(buff))
        case gameplayLib.Position.DropDeck => gamePlayGroundValuesThatSkillEffectCanChange.dropDeck = gamePlayGroundValuesThatSkillEffectCanChange.dropDeck.map(_.addBuff(buff))
        case theWhere if theWhere == gameplayLib.Position.HandCards || theWhere == gameplayLib.Position.MyHandCards || theWhere == gameplayLib.Position.OtherHandCards || theWhere == gameplayLib.Position.OpponentHandCards => {
          addPlayers.foreach(who => {
            val newOneStatus = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus(who).addHandCardBuff(buff)
            gamePlayGroundValuesThatSkillEffectCanChange.playersStatus += who -> newOneStatus
          })
        }
        case theWhere if theWhere == gameplayLib.Position.SpawnCard || theWhere == gameplayLib.Position.MySpawnCards || theWhere == gameplayLib.Position.OtherSpawnCards || theWhere == gameplayLib.Position.OpponentSpawnCards =>
          newSpawningCard = spawningCard.map(_.addBuff(buff))
      }
    })
    (gamePlayGroundValuesThatSkillEffectCanChange, newSpawningCard)
  }
}

case class DelBuffFromPlayer(toWho: Who, buffEffect: BuffEffect) extends SkillEffect {
  override def activeFormEffect(gid: Int, lastResult: FormSkillResult): FormSkillResult = lastResult

  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = {
    val delPlayer = SkillEffect.getTheNamesWhoToEffect(gamePlayGroundValuesThatSkillEffectCanChange, toWho, caster, obj)
    delPlayer.foreach(who => {
      val newOneStatus = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus(who).delBuffFromPlayer(buffEffect)
      gamePlayGroundValuesThatSkillEffectCanChange.playersStatus += who -> newOneStatus
    })

    (gamePlayGroundValuesThatSkillEffectCanChange, spawningCard)
  }

}

case class DelBuffFromCard(toWho: Who, wheres: Seq[Position], buffEffect: BuffEffect) extends SkillEffect {
  override def activeFormEffect(gid: Int, lastResult: FormSkillResult): FormSkillResult = lastResult

  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = {
    val delPlayers = SkillEffect.getTheNamesWhoToEffect(gamePlayGroundValuesThatSkillEffectCanChange, toWho, caster, obj)

    var newSpawningCard = spawningCard
    wheres.foreach(x => {
      val aWhere = x
      aWhere match {
        case gameplayLib.Position.DrawDeck => gamePlayGroundValuesThatSkillEffectCanChange.drawDeck = gamePlayGroundValuesThatSkillEffectCanChange.drawDeck.map(_.delBuff(buffEffect))
        case gameplayLib.Position.DropDeck => gamePlayGroundValuesThatSkillEffectCanChange.dropDeck = gamePlayGroundValuesThatSkillEffectCanChange.dropDeck.map(_.delBuff(buffEffect))
        case theWhere if theWhere == gameplayLib.Position.HandCards || theWhere == gameplayLib.Position.MyHandCards || theWhere == gameplayLib.Position.OtherHandCards || theWhere == gameplayLib.Position.OpponentHandCards => {
          delPlayers.foreach(who => {
            val newOneStatus = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus(who).delHandCardBuff(buffEffect)
            gamePlayGroundValuesThatSkillEffectCanChange.playersStatus += who -> newOneStatus
          })
        }
        case theWhere if theWhere == gameplayLib.Position.SpawnCard || theWhere == gameplayLib.Position.MySpawnCards || theWhere == gameplayLib.Position.OtherSpawnCards || theWhere == gameplayLib.Position.OpponentSpawnCards =>
          newSpawningCard = spawningCard.map(_.delBuff(buffEffect))
      }
    })
    (gamePlayGroundValuesThatSkillEffectCanChange, newSpawningCard)
  }
}


case class AddExtraCard(toWho: Who, wheres: Seq[Position], cardIds: Seq[Int], cardStandType: CardStandType) extends SkillEffect {
  override def activeFormEffect(gid: Int, lastResult: FormSkillResult): FormSkillResult = lastResult

  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = {

    val addCardPlayers = SkillEffect.getTheNamesWhoToEffect(gamePlayGroundValuesThatSkillEffectCanChange, toWho, caster, obj)
    var newSpawningCard = spawningCard
    val oldGenId = gamePlayGroundValuesThatSkillEffectCanChange.genIdForCard
    val newGenId = oldGenId + cardIds.length
    val tuples: Seq[(Int, Int)] = cardIds.zip(oldGenId until newGenId)
    val cardsToAdd = tuples.map(x => Card(x._1, x._2, 3, 4, cardStandType, None, Nil, Nil))

    gamePlayGroundValuesThatSkillEffectCanChange.genIdForCard = newGenId

    wheres.foreach(x => {
      val aWhere = x
      aWhere match {
        case gameplayLib.Position.DrawDeck => gamePlayGroundValuesThatSkillEffectCanChange.drawDeck = gamePlayGroundValuesThatSkillEffectCanChange.drawDeck ++ cardsToAdd
        case gameplayLib.Position.DropDeck => gamePlayGroundValuesThatSkillEffectCanChange.dropDeck = gamePlayGroundValuesThatSkillEffectCanChange.dropDeck ++ cardsToAdd
        case theWhere if theWhere == gameplayLib.Position.HandCards || theWhere == gameplayLib.Position.MyHandCards || theWhere == gameplayLib.Position.OtherHandCards || theWhere == gameplayLib.Position.OpponentHandCards => {
          addCardPlayers.foreach(who => {
            val newOneStatus = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus(who).addHandCard(cardsToAdd)
            gamePlayGroundValuesThatSkillEffectCanChange.playersStatus += who -> newOneStatus
          })
        }
        case theWhere if theWhere == gameplayLib.Position.SpawnCard || theWhere == gameplayLib.Position.MySpawnCards || theWhere == gameplayLib.Position.OtherSpawnCards || theWhere == gameplayLib.Position.OpponentSpawnCards =>
          newSpawningCard = spawningCard ++ cardsToAdd
      }
    })
    (gamePlayGroundValuesThatSkillEffectCanChange, newSpawningCard)
  }
}

case class MoveCastCard(cardId: Int, fromWheres: Seq[Position], fromWho: Who, toWhere: Position, toWho: Who, maxNum: Int) extends SkillEffect {
  override def activeFormEffect(gid: Int, lastResult: FormSkillResult): FormSkillResult = lastResult

  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = {

    val fromPlayers = SkillEffect.getTheNamesWhoToEffect(gamePlayGroundValuesThatSkillEffectCanChange, fromWho, caster, obj)
    val toThePlayer = SkillEffect.getTheNamesWhoToEffect(gamePlayGroundValuesThatSkillEffectCanChange, toWho, caster, obj).headOption
    var theMoveCard: Option[Card] = None
    var newSpawningCard = spawningCard
    fromWheres.foreach(x => {
      val aWhere = x
      aWhere match {
        case gameplayLib.Position.DrawDeck =>
          val cards = gamePlayGroundValuesThatSkillEffectCanChange.drawDeck
          gamePlayGroundValuesThatSkillEffectCanChange.drawDeck = cards.filterNot(_.genId == gId)
          theMoveCard = cards.find(_.genId == gId)

        case gameplayLib.Position.DropDeck =>
          val cards = gamePlayGroundValuesThatSkillEffectCanChange.dropDeck
          gamePlayGroundValuesThatSkillEffectCanChange.dropDeck = cards.filterNot(_.genId == gId)
          theMoveCard = cards.find(_.genId == gId)
        case theWhere if theWhere == gameplayLib.Position.HandCards || theWhere == gameplayLib.Position.MyHandCards || theWhere == gameplayLib.Position.OtherHandCards || theWhere == gameplayLib.Position.OpponentHandCards => {
          fromPlayers.foreach(who => {
            val tuple = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus(who).drawOutCardByGenId(gId)
            val newOneStatus = tuple._1
            gamePlayGroundValuesThatSkillEffectCanChange.playersStatus += who -> newOneStatus
            theMoveCard =
              tuple._2 match {
                case None => theMoveCard
                case _ => tuple._2
              }
          })
        }
        case theWhere if theWhere == gameplayLib.Position.SpawnCard || theWhere == gameplayLib.Position.MySpawnCards || theWhere == gameplayLib.Position.OtherSpawnCards || theWhere == gameplayLib.Position.OpponentSpawnCards =>
          newSpawningCard = spawningCard.filterNot(_.genId == gId)
          theMoveCard = spawningCard.find(_.genId == gId)
      }
    })

    toWhere match {
      case gameplayLib.Position.DrawDeck =>
        val cards = gamePlayGroundValuesThatSkillEffectCanChange.drawDeck

        gamePlayGroundValuesThatSkillEffectCanChange.drawDeck = Card.randAddACard(cards, theMoveCard)


      case gameplayLib.Position.DropDeck =>
        val cards = gamePlayGroundValuesThatSkillEffectCanChange.dropDeck
        gamePlayGroundValuesThatSkillEffectCanChange.dropDeck = Card.randAddACard(cards, theMoveCard)
      case theWhere if theWhere == gameplayLib.Position.HandCards || theWhere == gameplayLib.Position.MyHandCards || theWhere == gameplayLib.Position.OtherHandCards || theWhere == gameplayLib.Position.OpponentHandCards => {
        if (toThePlayer.isDefined) {
          val newOneStatus = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus(toThePlayer.get).addHandCard(theMoveCard.toSeq)
          gamePlayGroundValuesThatSkillEffectCanChange.playersStatus += toThePlayer.get -> newOneStatus
        }
      }

      case theWhere if theWhere == gameplayLib.Position.SpawnCard || theWhere == gameplayLib.Position.MySpawnCards || theWhere == gameplayLib.Position.OtherSpawnCards || theWhere == gameplayLib.Position.OpponentSpawnCards =>
        newSpawningCard = Card.randAddACard(spawningCard, theMoveCard)
    }
    (gamePlayGroundValuesThatSkillEffectCanChange, newSpawningCard)
  }

}

case class DropMinHandCard(toWho: Who, num: Int) extends SkillEffect {
  override def activeFormEffect(gid: Int, lastResult: FormSkillResult): FormSkillResult = lastResult

  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = {
    val dropPlayers = SkillEffect.getTheNamesWhoToEffect(gamePlayGroundValuesThatSkillEffectCanChange, toWho, caster, obj)
    dropPlayers.foreach(who => {
      val tuple: (OnePlayerStatus, Seq[Card]) = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus(who).dropCards(false, num)
      val newOneStatus = tuple._1
      gamePlayGroundValuesThatSkillEffectCanChange.playersStatus += who -> newOneStatus
      gamePlayGroundValuesThatSkillEffectCanChange.dropDeck = tuple._2 ++ gamePlayGroundValuesThatSkillEffectCanChange.dropDeck
    })
    (gamePlayGroundValuesThatSkillEffectCanChange, spawningCard)
  }

}

case class DropMaxHandCard(toWho: Who, num: Int) extends SkillEffect {
  override def activeFormEffect(gid: Int, lastResult: FormSkillResult): FormSkillResult = lastResult

  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = {
    val dropPlayers = SkillEffect.getTheNamesWhoToEffect(gamePlayGroundValuesThatSkillEffectCanChange, toWho, caster, obj)
    dropPlayers.foreach(who => {
      val tuple: (OnePlayerStatus, Seq[Card]) = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus(who).dropCards(false, num)
      val newOneStatus = tuple._1
      gamePlayGroundValuesThatSkillEffectCanChange.playersStatus += who -> newOneStatus
      gamePlayGroundValuesThatSkillEffectCanChange.dropDeck = tuple._2 ++ gamePlayGroundValuesThatSkillEffectCanChange.dropDeck
    })
    (gamePlayGroundValuesThatSkillEffectCanChange, spawningCard)
  }
}

case class ExtraDrawCard(toWho: Who, num: Int) extends SkillEffect {
  override def activeFormEffect(gid: Int, lastResult: FormSkillResult): FormSkillResult = lastResult

  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = {
    val drawPlayers = SkillEffect.getTheNamesWhoToEffect(gamePlayGroundValuesThatSkillEffectCanChange, toWho, caster, obj)

    drawPlayers.foreach(
      who => {
        val beforeDrawDeck = gamePlayGroundValuesThatSkillEffectCanChange.drawDeck
        val beforeDropDeck = gamePlayGroundValuesThatSkillEffectCanChange.dropDeck
        num match {
          case n if n <= beforeDrawDeck.length =>
            val cardsTuple = beforeDrawDeck.splitAt(n)
            val newOneStatus = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus(who).addHandCard(cardsTuple._1)
            gamePlayGroundValuesThatSkillEffectCanChange.playersStatus += who -> newOneStatus
            gamePlayGroundValuesThatSkillEffectCanChange.drawDeck = cardsTuple._2

          case n if n >= beforeDrawDeck.length + beforeDropDeck.length =>
            val newOneStatus = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus(who).addHandCard(beforeDrawDeck ++ beforeDropDeck)
            gamePlayGroundValuesThatSkillEffectCanChange.playersStatus += who -> newOneStatus
            gamePlayGroundValuesThatSkillEffectCanChange.drawDeck = Nil
            gamePlayGroundValuesThatSkillEffectCanChange.dropDeck = Nil
          case n if n < beforeDrawDeck.length + beforeDropDeck.length && n > beforeDrawDeck.length =>
            val cardsTuple = (beforeDrawDeck ++ Card.shuffleCard(beforeDropDeck)).splitAt(n)
            val newOneStatus = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus(who).addHandCard(cardsTuple._1)
            gamePlayGroundValuesThatSkillEffectCanChange.playersStatus += who -> newOneStatus
            gamePlayGroundValuesThatSkillEffectCanChange.drawDeck = cardsTuple._2
        }
      }
    )
    (gamePlayGroundValuesThatSkillEffectCanChange, spawningCard)
  }
}


case class GiveHandCard(FromWho: Who, toWho: Who, fromMax: Boolean, num: Int) extends SkillEffect {
  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = {
    val fromPlayers = SkillEffect.getTheNamesWhoToEffect(gamePlayGroundValuesThatSkillEffectCanChange, FromWho, caster, obj)
    val toThePlayer = SkillEffect.getTheNamesWhoToEffect(gamePlayGroundValuesThatSkillEffectCanChange, toWho, caster, obj).head
    var theGiveCards = Nil: Seq[Card]
    fromPlayers.foreach(who => {
      val tuple: (OnePlayerStatus, Seq[Card]) = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus(who).dropCards(fromMax, num)
      val newOneStatus = tuple._1
      gamePlayGroundValuesThatSkillEffectCanChange.playersStatus += who -> newOneStatus
      theGiveCards = tuple._2 ++ theGiveCards
    })
    val status: OnePlayerStatus = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus(toThePlayer).addHandCard(theGiveCards)
    gamePlayGroundValuesThatSkillEffectCanChange.playersStatus += toThePlayer -> status
    (gamePlayGroundValuesThatSkillEffectCanChange, spawningCard)
  }

  override def activeFormEffect(gid: Int, lastResult: FormSkillResult): FormSkillResult = lastResult
}

case class DestroyTheSelfCard(toWho: Who, wheres: Seq[Position], cardId: Int) extends SkillEffect {
  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = {
    val toPlayers = SkillEffect.getTheNamesWhoToEffect(gamePlayGroundValuesThatSkillEffectCanChange, toWho, caster, obj)
    var newSpawningCard = spawningCard

    wheres.foreach(x => {
      val aWhere = x
      aWhere match {
        case gameplayLib.Position.DrawDeck =>
          val cards = gamePlayGroundValuesThatSkillEffectCanChange.drawDeck
          gamePlayGroundValuesThatSkillEffectCanChange.drawDeck = cards.filterNot(_.genId == gId)
          val maybeCard: Option[Card] = cards.find(_.genId == gId)
          gamePlayGroundValuesThatSkillEffectCanChange.destroyedDeck = maybeCard.get +: gamePlayGroundValuesThatSkillEffectCanChange.destroyedDeck

        case gameplayLib.Position.DropDeck =>
          val cards = gamePlayGroundValuesThatSkillEffectCanChange.dropDeck
          gamePlayGroundValuesThatSkillEffectCanChange.dropDeck = cards.filterNot(_.genId == gId)
          val maybeCard: Option[Card] = cards.find(_.genId == gId)
          gamePlayGroundValuesThatSkillEffectCanChange.destroyedDeck = maybeCard.get +: gamePlayGroundValuesThatSkillEffectCanChange.destroyedDeck


        case theWhere if theWhere == gameplayLib.Position.HandCards || theWhere == gameplayLib.Position.MyHandCards || theWhere == gameplayLib.Position.OtherHandCards || theWhere == gameplayLib.Position.OpponentHandCards => {
          toPlayers.foreach(who => {
            val tuple = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus(who).drawOutCardByGenId(gId)
            val newOneStatus = tuple._1
            gamePlayGroundValuesThatSkillEffectCanChange.playersStatus += who -> newOneStatus
            val maybeCard: Option[Card] = tuple._2
            gamePlayGroundValuesThatSkillEffectCanChange.destroyedDeck = maybeCard.get +: gamePlayGroundValuesThatSkillEffectCanChange.destroyedDeck
          }
          )
        }
        case theWhere if theWhere == gameplayLib.Position.SpawnCard || theWhere == gameplayLib.Position.MySpawnCards || theWhere == gameplayLib.Position.OtherSpawnCards || theWhere == gameplayLib.Position.OpponentSpawnCards =>
          newSpawningCard = spawningCard.filterNot(_.genId == gId)
          val maybeCard: Option[Card] = spawningCard.find(_.genId == gId)
          gamePlayGroundValuesThatSkillEffectCanChange.destroyedDeck = maybeCard.get +: gamePlayGroundValuesThatSkillEffectCanChange.destroyedDeck
      }
    })
    (gamePlayGroundValuesThatSkillEffectCanChange, newSpawningCard)
  }

  override def activeFormEffect(gid: Int, lastResult: FormSkillResult): FormSkillResult = lastResult
}

case class DestroyMaxOrMinCard(isMax: Boolean, toWho: Who, wheres: Seq[Position], num: Int) extends SkillEffect {
  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = {
    val toPlayers = SkillEffect.getTheNamesWhoToEffect(gamePlayGroundValuesThatSkillEffectCanChange, toWho, caster, obj)
    var newSpawningCard = spawningCard
    wheres.foreach(x => {
      val aWhere = x
      aWhere match {


        case theWhere if theWhere == gameplayLib.Position.HandCards || theWhere == gameplayLib.Position.MyHandCards || theWhere == gameplayLib.Position.OtherHandCards || theWhere == gameplayLib.Position.OpponentHandCards => {
          toPlayers.foreach(who => {
            val tuple = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus(who).dropCards(isMax, num)
            val newOneStatus = tuple._1
            gamePlayGroundValuesThatSkillEffectCanChange.playersStatus += who -> newOneStatus
            val cards = tuple._2
            gamePlayGroundValuesThatSkillEffectCanChange.destroyedDeck = cards ++ gamePlayGroundValuesThatSkillEffectCanChange.destroyedDeck
          }
          )
        }
        case theWhere if theWhere == gameplayLib.Position.SpawnCard || theWhere == gameplayLib.Position.MySpawnCards || theWhere == gameplayLib.Position.OtherSpawnCards || theWhere == gameplayLib.Position.OpponentSpawnCards =>
          val characters = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus(caster).characters
          val sCards = Card.sortHandCard(spawningCard, characters)
          val sCards2 = if (isMax) sCards.reverse else sCards
          val tuple = sCards2.splitAt(num)
          newSpawningCard = tuple._2

          gamePlayGroundValuesThatSkillEffectCanChange.destroyedDeck = tuple._1 ++ gamePlayGroundValuesThatSkillEffectCanChange.destroyedDeck

        case _ => print("不可配置位置")
      }
    })
    (gamePlayGroundValuesThatSkillEffectCanChange, newSpawningCard)
  }

  override def activeFormEffect(gid: Int, lastResult: FormSkillResult): FormSkillResult = lastResult
}

case class FirstSeatNextRound(toWho: Who) extends SkillEffect {
  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):

  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = ???

  //  {
  //    val toThePlayer = SkillEffect.getTheNamesWhoToEffect(gamePlayGroundValuesThatSkillEffectCanChange, toWho, caster, obj).head
  //    val}


  override def activeFormEffect(gid: Int, lastResult: FormSkillResult): FormSkillResult = lastResult
}

case class FormEffect_CopyCard(num: Int) extends SkillEffect {
  override def activeEffect(gId: Int, caster: String, obj: Option[String], spawningCard: Seq[Card],
                            gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange):
  (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = (gamePlayGroundValuesThatSkillEffectCanChange, spawningCard)


  override def activeFormEffect(gid: Int, lastResult: FormSkillResult): FormSkillResult = {
    val formCardsSeq = lastResult.formSeq
    val newForms = formCardsSeq.map(formCards => {
      val copyCard = formCards.find(card => card.genId == gid && card.standType != CardStandType.TempCopy)
      copyCard match {
        case None => formCards
        case Some(x) => formCards ++ x.formCopy(num)
      }
    })
    val resForm = formCardsSeq ++ newForms
    FormSkillResult(lastResult.thisShapeSeq, lastResult.opShapeSeq, resForm)
  }


}


sealed trait SkillEffect {
  def activeEffect(gid: Int, caster: String, obj: Option[String], spawningCard: Seq[Card], gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange): (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card])

  def activeFormEffect(gid: Int, lastResult: FormSkillResult): FormSkillResult
}

object SkillEffect {
  def activeSkillEffectToGamePlayGroundAndSpawnCards(casterToSeqEffect: Map[(String, Option[String], Int), Seq[SkillEffect]], gamePlayGround: GamePlayGroundValuesThatSkillEffectCanChange, opponent: Option[String], spawningCard: Seq[Card]): (GamePlayGroundValuesThatSkillEffectCanChange, Seq[Card]) = {

    casterToSeqEffect.foldLeft((gamePlayGround, spawningCard))((sAndG, x) => {
      val theSpawnCard = sAndG._2
      val theGamePlay = sAndG._1
      val caster = x._1._1
      val obj = x._1._2
      val genId = x._1._3
      val Effects: Seq[SkillEffect] = x._2
      Effects.foldLeft((theGamePlay, theSpawnCard))((tuple, effect) => {
        val s = tuple._2
        val g = tuple._1
        effect.activeEffect(genId, caster, obj, s, g)
      })
    })
  }

  def getTheNamesWhoToEffect(gamePlayGroundValuesThatSkillEffectCanChange: GamePlayGroundValuesThatSkillEffectCanChange, toWho: Who, caster: String, obj: Option[String]): Iterable[String] = {
    val players = gamePlayGroundValuesThatSkillEffectCanChange.playersStatus.keys
    toWho match {
      case This => Seq(caster)
      case Other => players.filter(x => x != caster)
      case Opponent => Seq(obj.getOrElse(caster))
      case All => players
    }
  }

  case class FormSkillResult(thisShapeSeq: Seq[Option[Shape]], opShapeSeq: Seq[Option[Shape]], formSeq: Seq[Seq[Card]])

  def activeSkillEffectToFormCard(caster: String, casterToSeqEffect: Map[(String, Option[String], Int), Seq[SkillEffect]], thisShapeNeedCounter: Option[Shape], opShapeNeedCounter: Option[Shape], formCards: Seq[Card]): FormSkillResult

  = {
    val thisShapeSeq = Seq(thisShapeNeedCounter)
    val opShapeSeq = Seq(opShapeNeedCounter)
    val formSeq = Seq(formCards)
    val oResult = FormSkillResult(thisShapeSeq, opShapeSeq, formSeq)
    casterToSeqEffect.foldLeft(oResult)(
      (res, aKv) => if (caster == aKv._1._1) {
        val effects = aKv._2
        val gid = aKv._1._3
        effects.foldLeft(res)(
          (fRes, aSkillEffect) =>
            aSkillEffect match {
              case FormEffect_CopyCard(_) => aSkillEffect.activeFormEffect(gid, fRes)
              case _ => fRes
            }
        )
      } else res
    )
  }
}

