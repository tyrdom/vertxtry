package gameplayLib

object Config {
  val maxPoint = 10
  val minPoint = 1
  val minStraightNeed = 5
  val maxLength: Int = maxPoint - minPoint + 1
  val minLength = 1
  val levelArray: Array[(Int, Int)] = Array(
    (0, 1),
    (2, 2),
    (5, 3),
    (9, 4),
    (15, 5),
    (24, 6),
    (36, 7),
    (52, 8),
    (72, 9),
    (97, 10)
  )
}
