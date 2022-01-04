package terminal

class Terminal {
  private val HEIGHT = 24
  private var screenContents : Array[Array[Char]] = Array.fill(HEIGHT, 80)(' ')
  private var cursorX = 1
  private var cursorY = 1

  def getScreen() : Seq[String] = screenContents.map(_.mkString).toSeq
  def getCursorX() : Int = cursorX
  def getCursorY() : Int = cursorY
  def sendChar(char : Char) : Unit = if (char != '\u0000') then {
    screenContents(cursorY - 1)(cursorX - 1) = char
    if (cursorX < 80) then cursorX += 1
  }
}
