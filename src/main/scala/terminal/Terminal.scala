package terminal

import scala.collection.immutable.Queue
import java.time.temporal.Temporal

/** This terminal: * ignores tab characters * does not scroll - ignores line
  * feeds and cursor down commands when at the bottom of the screen * ignores SO
  * and SI codes * ignores XON and XOFF codes, and does not transmit them *
  * Ignores these sequences: CPR, DA, DECALN, DECDHL, DECDWL, DECID, DECKPAM,
  * DECLL, DECRC, DECREPTPARM, DECREQTPARM, DECSC, DECSTBM, DECSWL, DECTST, DSR
  */
class Terminal(x: Int = 1, y: Int = 1, initialScreenContents: String = "") {
  private var cursor: Position = Position(x, y)
  private var inputBuffer: Queue[Char] = Queue()
  private var screen: Screen = Screen(initialScreenContents)

  def getScreen(): Seq[String] = screen.getContents()
  def getCursorX(): Int = cursor.x
  def getCursorY(): Int = cursor.y

  def receiveChar(char: Char): Unit = char match {
    case Terminal.NUL | Terminal.DEL => ()
    case c =>
      inputBuffer = inputBuffer :+ c
      processInputBuffer()
  }

  private def performAction(action: Terminal.Action) = action match {
    case Terminal.Action.Backspace      => cursor = cursor.left()
    case Terminal.Action.Linefeed       => cursor = cursor.down()
    case Terminal.Action.CarriageReturn => cursor = Position(1, cursor.y)
    case Terminal.Action.PrintCharacter(c) =>
      screen.printChar(cursor.x, cursor.y, c)
      cursor = cursor.right()
    case Terminal.Action.CursorUp(n)      => moveUp(n)
    case Terminal.Action.CursorDown(n)    => moveDown(n)
    case Terminal.Action.CursorRight(n)   => moveRight(n)
    case Terminal.Action.CursorLeft(n)    => moveLeft(n)
    case Terminal.Action.MoveCursor(x, y) => moveTo(x, y)
    case Terminal.Action.EraseToEndOfScreen =>
      screen.eraseToEndOfScreen(cursor.x, cursor.y)
    case Terminal.Action.EraseFromStartOfScreen =>
      screen.eraseFromStartOfScreen(cursor.x, cursor.y)
    case Terminal.Action.EraseScreen => screen.eraseScreen()
    case Terminal.Action.EraseToEndOfLine => screen.eraseToEndOfLine(cursor.x, cursor.y)
    case Terminal.Action.EraseFromStartOfLine => screen.eraseFromStartOfLine(cursor.x, cursor.y)
    case Terminal.Action.UnrecognizedSequence(seq) => {
      // DEBUG
      println("Unrecognized character sequence:")
      println(seq.mkString)
      println(seq.map(_.toInt))
    }
  }

  private def processInputBuffer(): Unit = {
    inputBuffer.dequeue match
      case (Terminal.BS, tail) =>
        performAction(Terminal.Action.Backspace)
        inputBuffer = tail
      case (Terminal.LF | Terminal.VT | Terminal.FF, tail) =>
        performAction(Terminal.Action.Linefeed)
        inputBuffer = tail
      case (Terminal.CR, tail) =>
        performAction(Terminal.Action.CarriageReturn)
        inputBuffer = tail
      case (Terminal.ESC, tail) => parseSequenceAfterEscape(tail)
      case (c, tail) if !c.isControl => {
        performAction(Terminal.Action.PrintCharacter(c))
        inputBuffer = tail
      }
      case (c, tail) =>
        performAction(Terminal.Action.UnrecognizedSequence(Seq(c)))
  }

  private def parseSequenceAfterEscape(tail: Queue[Char]): Unit =
    tail.dequeueOption match
      case Some('[', tail) => parseSequenceAfterCSI(tail, Seq('['), Seq(), 0)
      case Some(c, tail) =>
        performAction(Terminal.Action.UnrecognizedSequence(Seq('\u001b', c)))
      case None => ()

  private def parseSequenceAfterCSI(
      tail: Queue[Char],
      sequence: Seq[Char],
      parameters: Seq[Int],
      currentParameter: Int
  ): Unit = tail.dequeueOption match
    case Some('A', tail) =>
      performAction(Terminal.Action.CursorUp(currentParameter.max(1)))
      inputBuffer = tail
    case Some('B', tail) =>
      performAction(Terminal.Action.CursorDown(currentParameter.max(1)))
      inputBuffer = tail
    case Some('C', tail) =>
      performAction(Terminal.Action.CursorRight(currentParameter.max(1)))
      inputBuffer = tail
    case Some('D', tail) =>
      performAction(Terminal.Action.CursorLeft(currentParameter.max(1)))
      inputBuffer = tail
    case Some('H', tail) => {
      parameters.length match {
        case 0 =>
          if currentParameter == 0 then
            performAction(Terminal.Action.MoveCursor(1, 1))
        case 1 => {
          val x = currentParameter.max(1)
          val y = parameters(0).max(1)
          performAction(Terminal.Action.MoveCursor(x, y))
        }
        case n => {
          performAction(
            Terminal.Action.UnrecognizedSequence('\u001b' +: sequence :+ 'H')
          )
        }
      }
      inputBuffer = tail
    }
    case Some('J', tail) => {
      if (parameters.isEmpty) then
        currentParameter match {
          case 0 => performAction(Terminal.Action.EraseToEndOfScreen)
          case 1 => performAction(Terminal.Action.EraseFromStartOfScreen)
          case 2 => performAction(Terminal.Action.EraseScreen)
          case n => {
            performAction(
              Terminal.Action.UnrecognizedSequence('\u001b' +: sequence :+ 'J')
            )
          }
        }
      else
        performAction(
          Terminal.Action.UnrecognizedSequence('\u001b' +: sequence :+ 'J')
        )
      inputBuffer = tail
    }
    case Some('K', tail) =>
      currentParameter match {
        case 0 => performAction(Terminal.Action.EraseToEndOfLine)
        case 1 => performAction(Terminal.Action.EraseFromStartOfLine)
        case 2 => screen.eraseLine(cursor.y)
        case n => {
          performAction(
            Terminal.Action.UnrecognizedSequence('\u001b' +: sequence :+ 'K')
          )
        }
      }
      inputBuffer = tail
    case Some(';', tail) =>
      parseSequenceAfterCSI(
        tail,
        sequence :+ ';',
        parameters :+ currentParameter,
        0
      )
    case Some(n, tail) if n.isDigit =>
      parseSequenceAfterCSI(
        tail,
        sequence :+ n,
        parameters,
        10 * currentParameter + n.asDigit
      )
    case Some(c, tail) =>
      performAction(
        Terminal.Action.UnrecognizedSequence('\u001b' +: sequence :+ 'c')
      )
    case None => ()

  private def moveUp(n: Int = 1): Unit = {
    cursor = cursor.up(n)
  }

  private def moveDown(n: Int = 1): Unit = {
    cursor = cursor.down(n)
  }

  private def moveRight(n: Int = 1): Unit = {
    cursor = cursor.right(n)
  }

  private def moveLeft(n: Int = 1): Unit = {
    cursor = cursor.left(n)
  }

  // Ignores invalid positions
  private def moveTo(x: Int, y: Int): Unit = {
    if 1 <= x && x <= Terminal.WIDTH && 1 <= y && y <= Terminal.HEIGHT then {
      cursor = Position(x, y)
    } else {
      // DEBUG
      println(s"Illegal position: $x,$y")
    }
  }
}

object Terminal {
  val HEIGHT = 24
  val WIDTH = 80
  private val NUL = '\u0000'
  private val BS = '\u0008'
  private val DEL = '\u007f'
  private val LF = '\n' // same as

  private val VT = '\u000b'
  private val FF = '\u000c'
  private val CR = '\u000d'
  private val ESC = '\u001b'

  enum Action {
    case Backspace
    case Linefeed
    case CarriageReturn
    case PrintCharacter(c: Char)
    case CursorUp(distance: Int)
    case CursorDown(distance: Int)
    case CursorRight(distance: Int)
    case CursorLeft(distance: Int)
    case MoveCursor(x: Int, y: Int)
    case EraseToEndOfScreen
    case EraseFromStartOfScreen
    case EraseScreen
    case EraseToEndOfLine
    case EraseFromStartOfLine
    case UnrecognizedSequence(seq: Seq[Char])
  }
}
