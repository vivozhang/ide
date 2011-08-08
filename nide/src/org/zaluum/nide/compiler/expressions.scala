package org.zaluum.nide.compiler
sealed trait BinOp

sealed trait ExprType extends BoxType {
  val owner = null
  def nameStr: String
  lazy val name = Name(nameStr)
  lazy val fqName = Name("org.zaluum.expr." + nameStr)
  var params = Map[Name, ParamSymbol]()
  def lookupPort(a: Name) = ports.get(a)
  def lookupPortWithSuper(a: Name) = lookupPort(a)
  def lookupParam(a: Name) = params.get(a)
  def templateTree = null
}
sealed trait ResultExprType extends ExprType {
  val o = new PortSymbol(this, Name("o"), Point(0, 0), Out)
  ports += (o.name -> o)
  def outPort(v: ValSymbol) = v.findPortInstance(o).get
}
sealed abstract class UnaryExprType(val nameStr: String) extends ResultExprType {
  val a = new PortSymbol(this, Name("a"), Point(0, 0), In)
  ports += (a.name -> a)

  def unaryPortInstancesOf(v: ValSymbol) =
    (v.findPortInstance(a).get, v.findPortInstance(o).get)

}
sealed abstract class BinExprType(val nameStr: String) extends ResultExprType {
  val a = new PortSymbol(this, Name("a"), Point(0, 0), In)
  val b = new PortSymbol(this, Name("b"), Point(0, 0), In)
  ports += (a.name -> a)
  ports += (b.name -> b)
  def binaryPortInstancesOf(v: ValSymbol) =
    (v.findPortInstance(a).get, v.findPortInstance(b).get, v.findPortInstance(o).get)

}
sealed abstract class MathExprType(nameStr: String) extends BinExprType(nameStr)
sealed abstract class CmpExprType(nameStr: String) extends BinExprType(nameStr)
sealed abstract class ShiftExprType(nameStr: String) extends BinExprType(nameStr)
sealed abstract class EqualityExprType(nameStr: String) extends BinExprType(nameStr)
sealed abstract class BitBinExprType(nameStr: String) extends BinExprType(nameStr)
sealed abstract class CastExprType(nameStr: String) extends UnaryExprType(nameStr)

sealed abstract class TemplateExprType(val nameStr: String) extends ExprType {
  val requiredBlocks: Int
}
object IfExprType extends TemplateExprType("If") {
  val requiredBlocks = 2
  val cond = new PortSymbol(this, Name("cond"), Point(0, 0), In)
  ports += (cond.name -> cond)
  def condPort(v: ValSymbol) = v.findPortInstance(cond).get
}
object WhileExprType extends TemplateExprType("While") {
  val requiredBlocks = 1
  val end = new PortSymbol(this, Name("end"), Point(0, 0), Out)
  ports += (end.name -> end)
  def endPort(v: ValSymbol) = v.findPortInstance(end).get
}
trait SignatureExprType extends ExprType {
  val Sig = """(.+)(\(.*)""".r
  val signatureName = Name("signature")
  val signatureSymbol = new ParamSymbol(null, signatureName)
  params += (signatureName -> signatureSymbol)
}
sealed abstract class ThisExprType(val nameStr: String) extends SignatureExprType {
  val thiz = new PortSymbol(this, Name("this"), Point(0, 0), In)
  val thizOut = new PortSymbol(this, Name("thisOut"), Point(0, 0), Out)
  ports += (thiz.name -> thiz)
  ports += (thizOut.name -> thizOut)
  def thisPort(vs: ValSymbol) = vs.findPortInstance(thiz).get
  def thisOutPort(vs: ValSymbol) = vs.findPortInstance(thizOut).get

}
sealed abstract class StaticExprType(val nameStr: String) extends SignatureExprType {
  val className = Name("class")
  val classSymbol = new ParamSymbol(null, className)
  params += (className -> classSymbol)
}
object NewExprType extends StaticExprType("New") {
  val thiz = new PortSymbol(this, Name("this"), Point(0,0), Out)
  ports += (thiz.name -> thiz)
  def thisPort(vs: ValSymbol) = vs.findPortInstance(thiz).get
}
object InvokeExprType extends ThisExprType("Invoke")
object InvokeStaticExprType extends StaticExprType("InvokeStatic")
object GetFieldExprType extends ThisExprType("GetField") with ResultExprType
object GetStaticFieldExprType extends StaticExprType("GetStaticField") with ResultExprType
object LiteralExprType extends ResultExprType {
  val nameStr = "Literal"
  val paramName = Name("literal")
  val paramSymbol = new ParamSymbol(null, paramName)
  params += (paramName -> paramSymbol)
}

object ToByteType extends CastExprType("ToByte")
object ToShortType extends CastExprType("ToShort")
object ToCharType extends CastExprType("ToChar")
object ToIntType extends CastExprType("ToInt")
object ToLongType extends CastExprType("ToLong")
object ToFloatType extends CastExprType("ToFloat")
object ToDoubleType extends CastExprType("ToDouble")

object ShiftLeftExprType extends ShiftExprType("ShiftLeft")
object UShiftRightExprType extends ShiftExprType("UShiftRight")
object ShiftRightExprType extends ShiftExprType("ShiftRight")

object LtExprType extends CmpExprType("Lt")
object LeExprType extends CmpExprType("Le")
object GtExprType extends CmpExprType("Gt")
object GeExprType extends CmpExprType("Ge")

object EqExprType extends EqualityExprType("Eq")
object NeExprType extends EqualityExprType("Ne")

object MinusExprType extends UnaryExprType("Minus")
object NotExprType extends UnaryExprType("Not")

object AndExprType extends BitBinExprType("And")
object OrExprType extends BitBinExprType("Or")
object XorExprType extends BitBinExprType("Xor")

object AddExprType extends MathExprType("Add")
object SubExprType extends MathExprType("Sub")
object MulExprType extends MathExprType("Mul")
object DivExprType extends MathExprType("Div")
object RemExprType extends MathExprType("Rem")
object Expressions {
  val all = List(
    NewExprType,
    InvokeExprType,
    InvokeStaticExprType,
    GetFieldExprType,
    GetStaticFieldExprType,
    WhileExprType,
    IfExprType,
    LiteralExprType,
    ToByteType,
    ToShortType,
    ToCharType,
    ToIntType,
    ToLongType,
    ToFloatType,
    ToDoubleType,
    ShiftLeftExprType,
    UShiftRightExprType,
    ShiftRightExprType,
    LtExprType,
    LtExprType,
    LeExprType,
    GtExprType,
    GeExprType,
    EqExprType,
    NeExprType,
    OrExprType,
    AndExprType,
    XorExprType,
    MinusExprType,
    NotExprType,
    AddExprType,
    SubExprType,
    MulExprType,
    DivExprType,
    RemExprType) map { e ⇒ e.fqName -> e } toMap
  val templateExpressions = List(
    IfExprType,
    WhileExprType) map { e ⇒ e.fqName -> e } toMap
  def find(name: Name) = all.get(name)
  def isTemplateExpression(className: Name) = templateExpressions.contains(className)

}