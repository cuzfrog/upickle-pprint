package upickle

import scala.{PartialFunction => PF}
import language.experimental.macros
/**
 * A typeclass that allows you to serialize a type [[T]] to JSON, and
 * eventually to a string
 */
trait Writer[T]{def write: T => Js.Value}
object Writer{
  implicit def macroW[T]: Writer[T] = macro Macros.macroWImpl[T]
  /**
   * Helper class to make it convenient to create instances of [[Writer]]
   * from the equivalent function
   */
  def apply[T](_write: T => Js.Value): Writer[T] = new Writer[T]{
    val write = _write
  }

}
/**
 * A typeclass that allows you to deserialize a type [[T]] from JSON,
 * which can itself be read from a String
 */
trait Reader[T]{def read: PF[Js.Value, T]}
object Reader{
  implicit def macroR[T]: Reader[T] = macro Macros.macroRImpl[T]

  /**
   * Helper class to make it convenient to create instances of [[Reader]]
   * from the equivalent function
   */
  def apply[T](_read: PF[Js.Value, T]): Reader[T] = new Reader[T]{
    def read = _read
  }
}

/**
 * Classes that provides a mutable version of [[ReadWriter]], used to
 * allow serialization and deserialization of recursive data structure
 */
object Knot {

  class RW[T](var _write: T => Js.Value, var _read: PF[Js.Value, T]) extends Reader[T] with Writer[T] {
    def read = _read

    def write = _write

    def copyFrom(rw: Reader[T] with Writer[T]) = {
      _write = rw.write
      _read = rw.read
    }
  }

  class R[T](var _read: PF[Js.Value, T]) extends Reader[T] {
    def read = _read

    def copyFrom(rw: Reader[T]) = {
      _read = rw.read
    }
  }

  class W[T](var _write: T => Js.Value) extends Writer[T] {
    def write = _write

    def copyFrom(rw: Writer[T]) = {
      _write = rw.write
    }
  }
}

/**
 * Helper object that makes it convenient to create instances of bother
 * [[Reader]] and [[Writer]] at the same time.
 */
object ReadWriter {
  def apply[T](_write: T => Js.Value, _read: PF[Js.Value, T]): Writer[T] with Reader[T] = new Writer[T] with Reader[T]{
    def read = _read
    def write = _write
  }
}

/**
 * Basic functionality to be able to read and write objects. Kept as a trait so
 * other internal files can use it, while also mixing it into the `upickle`
 * package to form the public API
 */
trait Types{
  type ReadWriter[T] = Reader[T] with Writer[T]

  /**
   * Serialize an object of type [[T]] to a `String`
   */
  def write[T: Writer](expr: T): String = Json.write(writeJs(expr))
  /**
   * Serialize an object of type [[T]] to a `Js.Value`
   */
  def writeJs[T: Writer](expr: T): Js.Value = implicitly[Writer[T]].write(expr)
  /**
   * Deserialize a `String` object of type [[T]]
   */
  def read[T: Reader](expr: String): T = readJs[T](Json.read(expr))
  /**
   * Deserialize a `Js.Value` object of type [[T]]
   */
  def readJs[T: Reader](expr: Js.Value): T = implicitly[Reader[T]].read(expr)
}