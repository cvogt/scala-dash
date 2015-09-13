package org.cvogt.dash
import play.api.libs.json._
import org.cvogt.play.json._
import implicits.optionWithNull
import SingletonEncoder.simpleName
import implicits.formatSingleton

sealed trait SelectorTarget

case class Replacement(
  `type` : EntryType,
  regexp: String = "",
  replacement: String = ""
) extends SelectorTarget

object Replacement{
  implicit val jsonFormat: Format[Replacement] = Jsonx.formatCaseClass[Replacement]
}

sealed trait EntryType extends SelectorTarget
case object Annotation extends EntryType
case object Attribute extends EntryType
case object Binding extends EntryType
case object Builtin extends EntryType
case object Callback extends EntryType
case object Category extends EntryType
case object Class extends EntryType
case object Command extends EntryType
case object Component extends EntryType
case object Constant extends EntryType
case object Constructor extends EntryType
case object Define extends EntryType
case object Delegate extends EntryType
case object Diagram extends EntryType
case object Directive extends EntryType
case object Element extends EntryType
case object Entry extends EntryType
case object Enum extends EntryType
case object Environment extends EntryType
case object Error extends EntryType
case object Event extends EntryType
case object Exception extends EntryType
case object Extension extends EntryType
case object Field extends EntryType
case object File extends EntryType
case object Filter extends EntryType
case object Framework extends EntryType
case object Function extends EntryType
case object Global extends EntryType
case object Guide extends EntryType
case object Hook extends EntryType
case object Instance extends EntryType
case object Instruction extends EntryType
case object Interface extends EntryType
case object Keyword extends EntryType
case object Library extends EntryType
case object Literal extends EntryType
case object Macro extends EntryType
case object Method extends EntryType
case object Mixin extends EntryType
case object Modifier extends EntryType
case object Module extends EntryType
case object Namespace extends EntryType
case object Notation extends EntryType
case object Object extends EntryType
case object Operator extends EntryType
case object Option extends EntryType
case object Package extends EntryType
case object Parameter extends EntryType
case object Plugin extends EntryType
case object Procedure extends EntryType
case object Property extends EntryType
case object Protocol extends EntryType
case object Provider extends EntryType
case object Provisioner extends EntryType
case object Query extends EntryType
case object Record extends EntryType
case object Resource extends EntryType
case object Sample extends EntryType
case object Section extends EntryType
case object Service extends EntryType
case object Setting extends EntryType
case object Shortcut extends EntryType
case object Statement extends EntryType
case object Struct extends EntryType
case object Style extends EntryType
case object Subroutine extends EntryType
case object Tag extends EntryType
case object Test extends EntryType
case object Trait extends EntryType
case object Type extends EntryType
case object Union extends EntryType
case object Value extends EntryType
case object Variable extends EntryType
case object Word extends EntryType

object EntryType{
  implicit val jsonFormat: Format[EntryType] = Jsonx.formatSealed[EntryType]
}
object SelectorTarget{
  implicit val jsonFormat: Format[SelectorTarget] = Jsonx.formatSealed[SelectorTarget]
}
