package wcs.scala

import scala.collection.immutable
import COM.FutureTense.Interfaces.ICS
import COM.FutureTense.Interfaces.IList
import wcs.java.{ Env => JEnv }
import java.util.Date

/**
 * Scala API on top of ICS
 */

class Env(_ics: ICS) extends JEnv(_ics) {

  /**
   * the value or a empty string if none
   */
  def apply(variable: String) = get(variable).getOrElse("")

  /**
   * the value or an empty string if note string if none
   */
  def apply(list: Symbol, field: String) = get(list, field).getOrElse("")

  
  /**
   * apply to a list with default value
   */
  def apply(list: Symbol): String = apply(list, "value")
  
  /**
   * the value or an empty string if none
   */
  def apply(list: Symbol, pos: Int, field: String) = get(list, pos, field).getOrElse("")

  /**
   * Check if a variable exists
   */
  def exist(variable: String) = isVariable(variable)

  /**
   * Check if a variable exists
   */
  def exist(list: Symbol) = isList(list.name)
  
  
  /**
   * Check if a variable exists
   */
  def exist(list: Symbol, field: String) = isField(list.name, field)


  /**
   * Return the optional value of the variable
   */
  def get(variable: String): Option[String] = {
    val v = getString(variable)
    if (v == null)
      None
    else
      Some(v)
  }

  /**
   * Return the optional value of the variable
   */
  def get(list: Symbol, field: String): Option[String] = {
    val v = getString(list.name, field)
    if (v == null)
      None
    else
      Some(v)
  }

  /**
   * Return the optional value of the variable
   */
  def get(list: Symbol, pos: Int, field: String): Option[String] = {
    val v = getString(list.name, pos, field)
    if (v == null)
      None
    else
      Some(v)
  }
  
  /**
   * Return the optional value of the variable
   */
  def asDate(variable: String): Option[Date] = {
    val v = getDate(variable)
    if (v == null)
      None
    else
      Some(v)
  }

  /**
   * Return the optional value of the variable
   */
  def asDate(list: Symbol, field: String): Option[Date] = {
    val v = getDate(list.name, field)
    if (v == null)
      None
    else
      Some(v)
  }

  /**
   * Return the optional value of the variable
   */
  def asDate(list: Symbol, pos: Int, field: String): Option[Date] = {
    val v = getDate(list.name, pos, field)
    if (v == null)
      None
    else
      Some(v)
  }

  /**
   * Return the optional value of the variable
   */
  def asLong(variable: String): Option[Long] = {
    val v = getLong(variable)
    if (v == null)
      None
    else
      Some(v)
  }

  /**
   * Return the optional value of the variable
   */
  def asLong(list: Symbol, field: String): Option[Long] = {
    val v = getLong(list.name, field)
    if (v == null)
      None
    else
      Some(v)
  }

  /**
   * Return the optional value of the variable
   */
  def asLong(list: Symbol, pos: Int, field: String): Option[Long] = {
    val v = getLong(list.name, pos, field)
    if (v == null)
      None
    else
      Some(v)
  }

  /**
   * set a variable
   */
  def update(s: String, v: String) {
    ics.SetVar(s, v)
  }

  /**
   * Range of a list
   */
  def range(list: Symbol) = {
    val l = ics.GetList(list.name)
    if (l == null)
      0 to -1
    else
      1 to l.numRows()
  }

  /**
   * Return a ilists as Seq of Maps
   */
  def list(list: String): Seq[Map[String, String]] = {

    val ls = ics.GetList(list)
    if (ls == null)
      List[Map[String, String]]()
    else {
      val l = for (i <- 1 to ls.numRows) yield {
        ls.moveTo(i)
        val ll = for (j <- 0 to ls.numColumns - 1) yield {
          val name = ls.getColumnName(j)
          val value = ls.getValue(name)
          name -> value
        }
        ll.toMap
      }
      l.toSeq
    }
  }

}
