package gameplayLib

case class SpawnedCard(who: Int, cards: Seq[Card])

case class needCounter(shape: Shape, counterHistorySpawn: Seq[SpawnedCard])

//needCounterShape：需要对抗的牌型，如果有出牌权，那么就需要对抗此Shape
//counterHistorySpawn：对抗的历史出牌，如果有需要对抗时对抗失败，则消灭这个needCounter，触发一些效果，对抗成功则把needCounter加入自己出的牌shape的点数，更新shape再转移给其他玩家
case class OnePlayerCardsStatus(handCards: Seq[Card], needCounters: needCounter) //玩家牌相关的状态：
//handCards ：手上的牌
//needCounters：为空而有牌权，则出牌创建needCounter给指定玩家，如果不为空则需要出牌Counter

object Phrase extends Enumeration { //阶段分类
type Phrase = Value
  val Draw = Value
  val Check = Value
  val Spawn = Value
  val Damage = Value
}

object Position extends Enumeration { //位置分类
type Position = Value
  val DrawDeck = Value
  val DrpDeck = Value
  val MySpawnCards = Value
  val MyHandCards = Value
  val OtherSpawnCards = Value
  val OtherHandCards = Value
}


case class GameStatus(var drawDeck: Seq[Card], //抽牌堆，公共一个 ，如果没有牌，则
                      var dropDeck: Seq[Card], //弃牌堆，公共一个
                      var destroyedDeck: Seq[Card], //毁掉的牌，不在循环
                      var playersHandAndNeedCounter: Map[(String, Int), OnePlayerCardsStatus], // 座位号 玩家牌状态，可以用于多于两个人的情况
                      characterPool: Seq[Character],

                      var turn: Int, //回合，一次轮换出牌对象为一回合

                      var round: Int, //轮，一方打完牌再弃牌重新抽牌为1轮

                      var spawnRight: Int,
                      var playerStatus: Map[Int, PlayerStatus],
                      maxNum: Int, // 最大的座位数，下一位为第1
                      var Outers: Seq[Int] //被淘汰的选手的位置
                     )

case class PlayerStatus(var hp: Int, var atk: Int, var defence: Int, var buffs: Seq[Buff], var characters: Seq[Character]) //除了位置，其他均可变

class PlayGround(gameStatus: GameStatus) { //每个房间需new1个新的playground

  def initPlayGround(playerNum: Int, characters: Seq[Character]): Unit = ??? //初始化玩家状态的过程

  def initCharacterPool() = ???

  def initDeck() = ???

  def initHandCard() = ???

  def initSeat() = ???
}
