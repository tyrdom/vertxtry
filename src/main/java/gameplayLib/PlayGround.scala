package gameplayLib

import com.sun.deploy.util.SyncFileAccess.RandomAccessFileLock

import scala.util.Random

case class SpawnedCard(who: String, cards: Seq[Card])

case class needCounter(shape: Shape, counterHistorySpawn: Seq[SpawnedCard])

//needCounterShape：需要对抗的牌型，如果有出牌权，那么就需要对抗此Shape
//counterHistorySpawn：对抗的历史出牌，如果有需要对抗时对抗失败，则消灭这个needCounter，触发一些效果，对抗成功则把needCounter加入自己出的牌shape的点数，更新shape再转移给其他玩家

case class OnePlayerStatus(var seat: Int = 0, //seat:座位号
                           var handCards: Seq[Card] = Nil: Seq[Card], //handCards ：手上的牌
                           var HP: Int = Config.initHitPoint, var attack: Int = 0, var defence: Int = 0, //最初的属性
                           var buffs: Seq[Buff] = Nil: Seq[Buff],
                           var characters: Seq[Character] = Nil: Seq[Character],
                           var needCounter: Option[needCounter] = None) //需要对抗的牌型和历史记录,如果为空则不需要按照对抗出牌


object Phrase extends Enumeration { //阶段分类
type Phrase = Value
  val Draw: Phrase = Value
  val Check: Phrase = Value
  val Spawn: Phrase = Value
  val Damage: Phrase = Value
}

object Position extends Enumeration { //位置分类
type Position = Value
  val DrawDeck: Position = Value
  val DrpDeck: Position = Value
  val MySpawnCards: Position = Value
  val MyHandCards: Position = Value
  val OtherSpawnCards: Position = Value
  val OtherHandCards: Position = Value
}


case class GamePlayGround(var drawDeck: Seq[Card] = Nil: Seq[Card], //抽牌堆，公共一个 ，如果没有牌，则
                          var dropDeck: Seq[Card] = Nil: Seq[Card], //弃牌堆，公共一个
                          var destroyedDeck: Seq[Card] = Nil: Seq[Card], //毁掉的牌，不在循环
                          var playersStatus: Map[String, OnePlayerStatus] = Map(), // 玩家id 座位号 玩家牌状态，可以用于多于两个人的情况
                          var characterPool: Seq[Character] = Nil: Seq[Character],
                          var totalTurn: Int = 0,
                          var turn: Int = 0, //回合，一次轮换出牌对象为一回合
                          var round: Int = 0, //轮，一方打完牌再弃牌重新抽牌为1轮
                          var spawnRight: Int = 0,
                          var maxPlayerNum: Int = 0, // 最大的座位数，下一位为第1
                          var nowPlayerNum: Int = 0, //当前未被淘汰的玩家
                          var Outers: Seq[String] = Nil: Seq[String] //被淘汰的选手顺序约后面越先被淘汰
                         ) { //每个房间需new1个新的playground

  def initPlayGround(players: Seq[String], charactersIds: Seq[Int]): Unit = {
    this.maxPlayerNum = players.count(_ => true)
    val pool: Seq[Character] = charactersIds.map(id => Character.initCharacter(id))

    this.characterPool = pool
    for (player <- players) {
      this.playersStatus += player -> OnePlayerStatus()
    }
  } //初始化玩家状态的过程

  def genPlayerChooseCharacterPools(chooseNum: Int): Map[String, Seq[Int]] = {
    val oPool = Random.shuffle(this.characterPool.map(_.id))
    val characterNum = oPool.count(_ => true)
    val rMap: Map[String, Seq[Int]] = Map()
    val realChooseNum: Int = chooseNum match {
      case cNum if cNum * this.maxPlayerNum <= characterNum => cNum
      case _ => characterNum / this.maxPlayerNum
    }


    rMap
  }

  def sliceToPieces[X](piecesNum: Int, pieceMaxRoom: Int, pool: Seq[X]): (Seq[Seq[X]],Seq[X]) = {
    val total = pieceMaxRoom * piecesNum
    val (sPool,rPool) = pool.splitAt(total)
    var temp: Map[Int, Seq[X]] = Map()
    for (i <- 0 until (piecesNum - 1)) {
      temp += (i -> Nil)
    }

    var index = 0
    for (x <- sPool) {
      val p = index % piecesNum
      val q = x +: temp(p)
      temp += (p -> q)
      index = index + 1
    }

    (temp.values.toSeq,rPool)

  }

  def initCharacterAndDeck() = ???

  def drawCards(maxNum: Int) = ???

  def initSeat() = ???
}
