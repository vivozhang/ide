package org.zaluum.nide.compiler

import javax.swing.JComponent
trait Symbol {
  def owner: Symbol
  def name: Name
  var decl: Tree = EmptyTree
  var tpe: Type = NoSymbol
  var scope: Scope = null
  override def toString = "Symbol("+ (if(name!=null) name.toString else "NoSymbol") +")"
}
trait Type extends Symbol
case object NoSymbol extends Symbol with Type {
  val owner = NoSymbol
  val name = null
}
class JavaType(val owner:Symbol, val name:Name) extends Symbol with Type{
  scope=owner.scope
  def descriptor = "L" + name.internal + ";"
}
class PrimitiveJavaType(owner:Symbol,name:Name, override val descriptor:String) extends JavaType(owner,name) 
class ClassJavaType(val owner:Symbol, val name:Name) extends Type {
  scope=owner.scope
}
class BoxTypeSymbol(
    val owner: Symbol, 
    val name: Name, 
    val superName:Option[Name], 
    val image:Option[String], 
    val visualClass:Option[Name],
    val abstractCl:Boolean=false) extends LocalScope(owner.scope) with Symbol with Type {
  var superSymbol:Option[BoxTypeSymbol] = None
  var source : String = "" // TODO
  def valsInOrder = boxes.values.toList.sortWith(_.name.str< _.name.str).asInstanceOf[List[ValSymbol]]
  def IOInOrder = ports.values.toList.sortWith(_.name.str<_.name.str).asInstanceOf[List[IOSymbol]]
  def params = ports.values collect { case p : ParamSymbol => p }
  var executionOrder = List[ValSymbol]() 
  def fqName : Name = owner match {
    case bown:BoxTypeSymbol => Name(bown.fqName.str + "$" + name.str)
    case _ => name
  }
  def isLocal = owner.isInstanceOf[BoxTypeSymbol]
  override def toString = "BoxTypeSymbol(" + name.str +", super=" + superSymbol +")"
  override def lookupPort(name: Name): Option[Symbol] = 
    super.lookupPort(name) orElse (superSymbol flatMap {_.lookupPort(name)}) 
}

class ConnectionSymbol(val owner:Symbol, val name:Name, val from:Tree, val to:Tree) extends Symbol 
// TODO make two classes one that has values from the declaring tree and the other directly from symbol
class IOSymbol(val owner: BoxTypeSymbol, val name : Name, val dir: PortDir) extends Symbol {
  def box = owner
}
class PortSymbol(owner: BoxTypeSymbol, name: Name, val extPos:Point, dir:PortDir) extends IOSymbol(owner,name,dir) {  
  override def toString = "PortSymbol(" + name + ")"
}
class ParamSymbol(owner: BoxTypeSymbol, name:Name, val default:String, dir:PortDir) extends IOSymbol(owner,name,dir) {
  override def toString = "ParamSymbol(" + name + ")"  
}
class ValSymbol(val owner: Symbol, val name: Name) extends Symbol {
  var params = Map [ParamSymbol,Any]()
  override def toString = "ValSymbol(" + name + ")"
}
