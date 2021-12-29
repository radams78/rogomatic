class VT100 {
  private var screen : Array[Array[Char]] = Array.fill(24)(Array.fill(80)(' '))
  private var cursor : Cursor = Cursor(1, 1)
  
  def getScreen() : Seq[String] = screen.map(_.mkString)

  def sendChar(char : Char) : Unit = screen(cursor.y - 1)(cursor.x - 1) = char

  private class Cursor(val x : Int, val y : Int)
}