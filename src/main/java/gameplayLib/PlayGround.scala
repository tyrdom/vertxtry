package gameplayLib

case class SpawnedCard(who: Int, cards: Seq[Card])

case class needCounter(shape: Shape, counterHistorySpawn: Seq[SpawnedCard])

//needCounterShape：需要对抗的牌型，如果有出牌权，那么就需要对抗此Shape
//counterHistorySpawn：对抗的历史出牌，如果有需要对抗时对抗失败，则消灭这个needCounter，触发一些效果，对抗成功则把needCounter加入自己出的牌，更新shape再丢给其他玩家
case class OnePlayerCardsStatus(handCards: Seq[Card], needCounters: Seq[needCounter]) //玩家牌相关的状态：
//handCards ：手上的牌
//needCounters：为空而有牌权，则出牌创建needCounter给指定玩家，如果不为空则需要出牌Counter

case class State(var drawDeck: Seq[Card], //抽牌堆，公共一个 ，如果没有牌，则
                 var dropDeck: Seq[Card], //弃牌堆，公共一个
                 var destroyedDeck: Seq[Card], //毁掉的牌，不在循环
                 var playersHandAndNeedCounter: Map[Int, OnePlayerCardsStatus], // 座位号 玩家牌状态，可以用于多于两个人的情况
                 characterPool: Seq[Character], var turn: Int, var round: Int,
                 var spawnRight: Int,
                 var playerStatus: Map[Int, PlayerStatus],
                 order: Seq[Int])

case class PlayerStatus(var hp: Int, var atk: Int, var defence: Int, var buffs: Seq[Buff], var characters: Seq[Character]) //除了位置，其他均可变

class PlayGround(state: State) { //每个房间需new1个新的playground

  def initPlayGround(playerNum: Int, characters: Seq[Character]): Unit = ??? //初始化玩家状态的过程


}
