package org.zaluum.nide.compiler

import scala.collection.immutable.Stack
import scala.collection.mutable.Buffer
import java.io.StringWriter
trait SelectionSubject {
  def selectedTree: Tree
}
abstract class Tree extends Product with SelectionSubject {
  var line: Int = 0
  def clean() {
    line = 0
  }
  def hasSymbol = false
  def isDef = false
  def isEmpty = false
  def selectedTree = this
  private def findPath0(l: Int): (Option[Tree], Int) = {
    if (l <= 0) (None, 0)
    else if (l == 1) (Some(this), 1)
    else {
      var visited = 1
      for (c ← children) {
        val (t, visits) = c.findPath0(l - visited)
        visited += visits
        if (t.isDefined) {
          return (t, visited)
        }
      }
      (None, visited)
    }
  }
  def findPath(l: Int): Option[Tree] = findPath0(l)._1
  def assignLine(l: Int): Int = {
    this.line = l
    var x = l + 1
    for (c ← children) {
      x = c.assignLine(x)
    }
    x
  }
  def deepContains(t: Tree) = deepChildrenStream.contains(t)
  def deepChildrenStream: Stream[Tree] =
    for (c ← children.toStream; dc ← Stream.cons(c, c.deepChildrenStream)) yield dc

  def children: List[Tree] = {
      def subtrees(x: Any): List[Tree] = x match {
        case t: Tree      ⇒ List(t)
        case o: Option[_] ⇒ for (ot ← o.toList; st ← subtrees(ot)) yield st
        case xs: List[_]  ⇒ xs flatMap subtrees
        case _            ⇒ List()
      }
    productIterator.toList flatMap subtrees
  }

  override def hashCode(): Int = super.hashCode()

  override def equals(that: Any): Boolean = that match {
    case t: Tree ⇒ this eq t
    case _       ⇒ false
  }
  def print(depth: Int): String = {
      def print(a: Any): String = {
        a match {
          case t: Tree    ⇒ t.print(depth + 1)
          case l: List[_] ⇒ l.map { print(_) }.mkString("\n")
          case _          ⇒ (" " * (depth + 1)) + a.toString
        }
      }
    val prods = productIterator.toList
    (" " * depth) + this.productPrefix + "(" +
      (if (prods.isEmpty) ")"
      else {
        "\n" + (for (e ← prods) yield {
          print(e)
        }).mkString("\n") +
          "\n" + (" " * depth) + ")"
      })
  }
}
abstract class SymbolTree[S >: Null <: Symbol] extends Tree {
  var symbol: S = null
  def sym = symbol
  override def clean() {
    super.clean
    symbol = null
  }
}

// Transformer
object EditTransformer {
  def apply(e: Transformer ⇒ TreePF) = {
    new EditTransformer {
      val trans = e(this)
    }
  }
}
abstract class EditTransformer extends CopyTransformer with MapTransformer

trait MapTransformer extends Transformer {
  var map = Map[SelectionSubject, SelectionSubject]()
  abstract override def transform[A <: Tree](tree: A): A = {
    val transformed = super.transform(tree)
    map += (tree -> transformed)
    transformed
  }
}
trait CopyTransformer extends Transformer {
  val defaultTransform: TreePF = {
    case b: BoxDef ⇒
      atOwner(b.symbol) {
        b.copy(template = transform(b.template))
      }
    case t: Template ⇒
      t.copy(blocks = transformTrees(t.blocks),
        ports = transformTrees(t.ports))
    case b: Block ⇒
      b.copy(
        junctions = transformTrees(b.junctions),
        connections = transformTrees(b.connections),
        parameters = transformTrees(b.parameters),
        valDefs = transformTrees(b.valDefs))
    case PortDef(name, typeName, dir, inPos, extPos) ⇒
      PortDef(name, typeName, dir, inPos, extPos)
    case v: ValDef ⇒
      atOwner(v.symbol) {
        v.copy(params = transformTrees(v.params),
          template = transformOption(v.template))
      }
    case p: Param ⇒ p.copy()
    case c @ ConnectionDef(a, b, wp) ⇒
      ConnectionDef(transformOption(a), transformOption(b), wp)
    case PortRef(from, name, in) ⇒
      PortRef(transform(from), name, in)
    case ValRef(name)   ⇒ ValRef(name)
    case j: JunctionRef ⇒ j.copy()
    case j: Junction    ⇒ j.copy()
    case t: ThisRef     ⇒ ThisRef()
  }
}
abstract class Transformer extends OwnerHelper[Tree] {
  protected val defaultTransform: TreePF
  protected val trans: TreePF
  protected lazy val finalTrans = trans.orElse(defaultTransform)
  def apply(tree: Tree, initOwner: Symbol = null): Tree = {
    currentOwner = initOwner
    transform(tree)
  }
  def transform[A <: Tree](tree: A): A = finalTrans.apply(tree).asInstanceOf[A]
  def transformOption[A <: Tree](tree: Option[A]): Option[A] =
    tree map (transform(_))
  def transformTrees[A <: Tree](trees: List[A]): List[A] =
    trees mapConserve (transform(_))
}
// Traverser
abstract class Traverser(initSymbol: Symbol) extends OwnerHelper[Unit] {
  currentOwner = initSymbol
  def traverse(tree: Tree): Unit = {
    tree match {
      case b: BoxDef ⇒
        atOwner(b.symbol) {
          traverse(b.template)
        }
      case t: Template ⇒
        atOwner(t.symbol) {
          traverseTrees(t.ports)
          traverseTrees(t.blocks)
        }
      case b: Block ⇒
        atOwner(b.symbol) {
          traverseTrees(b.valDefs)
          traverseTrees(b.junctions)
          traverseTrees(b.connections)
          traverseTrees(b.parameters)
        }
      case v: ValDef ⇒
        atOwner(v.symbol) {
          traverseTrees(v.params)
          traverseOption(v.template)
        }
      case p: Param ⇒
      case ConnectionDef(a, b, waypoints) ⇒
        traverseOption(a)
        traverseOption(b)
      case p: PortDef ⇒
      case PortRef(tree, _, _) ⇒
        traverse(tree)
      case j: Junction    ⇒
      case j: JunctionRef ⇒
      case ValRef(_)      ⇒
      case ThisRef()      ⇒
    }
  }
  def traverseTrees(trees: List[Tree]) {
    trees foreach traverse
  }
  def traverseOption(o: Option[Tree]) {
    o foreach traverse
  }
}
object PrettyPrinter {
  def print(str: String, deep: Int) {
    println(new String(Array.fill(deep) { ' ' }) + str)
  }
  def print(trees: List[Tree], deep: Int) {
    trees.foreach { print(_, deep) }
  }
  def sym(tree: Tree) = tree match {
    case s: SymbolTree[_] ⇒ " sym= " + s.symbol
    case _                ⇒ "_"
  }
  def print(tree: Tree, deep: Int): Unit = tree match {
    case b: BoxDef ⇒
      print("BoxDef(" + b.pkg + " " + b.name + ", " + b.image, deep)
      print(b.guiSize.toString, deep + 1)
      print(b.template, deep + 1)
      print(")" + b.line + sym(b), deep)
    case t: Template ⇒
      print("Template( ", deep)
      print(t.blocks, deep + 1)
      print(t.ports, deep + 1)
      print(")" + t.line, deep)
    case b: Block ⇒
      print("Block( ", deep)
      print(b.valDefs, deep + 1)
      print(b.connections, deep + 1)
      print(b.junctions, deep + 1)
      print(b.parameters, deep + 1)
      print(")" + b.line, deep)
    case v: ValDef ⇒
      print("ValDef(" + List(v.name, v.pos, v.size, v.typeName).mkString(","), deep)
      print(v.params, deep + 1)
      print(v.template.toList, deep + 1)
      print(")" + v.line, deep)
    case p: Param ⇒
      print(p.toString + p.line, deep)
    case c @ ConnectionDef(a, b, wp) ⇒
      print("ConnectionDef(", deep)
      a foreach { print(_, deep + 1) }
      b foreach { print(_, deep + 1) }
      for (p ← wp) {
        print(p.toString, deep + 1)
      }
      print(")" + c.line, deep)
    case p @ PortDef(_, _, _, _, _) ⇒
      print(p.toString + p.line, deep)
    case p @ PortRef(tree, a, b) ⇒
      print("PortRef(", deep)
      print(tree, deep + 1)
      print(a + ", " + b, deep + 1)
      print(")" + p.line, deep)
    case _ ⇒ print(tree.toString + tree.line, deep)
  }
}
abstract class OwnerHelper[A] {
  protected var currentOwner: Symbol = null

  def atOwner(owner: Symbol)(traverse: ⇒ A): A = {
    val prevOwner = currentOwner
    currentOwner = owner
    val result = traverse
    currentOwner = prevOwner
    result
  }
}
// ----- tree node alternatives --------------------------------------

/* Definition */
object BoxDef {

  def emptyBox(name: String, pkg: String) = {
    val block = Block(junctions = List(),
      connections = List(),
      parameters = List(),
      valDefs = List())
    val template = Template(ports = List(), blocks = List(block), currentBlock = None)
    BoxDef(name = Name(name),
      pkg = Name(pkg),
      guiSize = Some(Dimension(250, 250)),
      image = None,
      initMethod = None,
      constructor = List(),
      template = template)
  }
}
case class BoxDef(name: Name, // simple name
                  pkg: Name,
                  guiSize: Option[Dimension],
                  image: Option[String],
                  initMethod: Option[String],
                  constructor: List[VarDecl],
                  template: Template) extends SymbolTree[BoxSymbol] {
  def transformThis(body: EditTransformer ⇒ BoxDef) = new EditTransformer() {
    val trans: TreePF = {
      case b: BoxDef if b == BoxDef.this ⇒ body(this)
    }
  }
  def editInitMethod(m: Option[String]) = transformThis { e ⇒
    copy(initMethod = m, template = e.transform(template))
  }
  def editConstructor(l: List[VarDecl]) = transformThis { e ⇒
    copy(constructor = l, template = e.transform(template))
  }
}
case class VarDecl(name: Name, tpeName: Name) extends SymbolTree[ParamDecl]
object Template {
  def emptyTemplate(blocks: Int) = {
    Template(List.fill(blocks) { Block.empty }, List(), None)
  }
}
case class Template(
  blocks: List[Block],
  ports: List[PortDef],
  currentBlock: Option[String]) extends SymbolTree[TemplateSymbol]
object Block {
  def empty = Block(List(), List(), List(), List())
}
case class Block(
  junctions: List[Junction],
  connections: List[ConnectionDef],
  parameters: List[Param],
  valDefs: List[ValDef]) extends SymbolTree[BlockSymbol]
object PortDir {
  def fromStr(str: String) = str match {
    case In.str    ⇒ In
    case Out.str   ⇒ Out
    case Shift.str ⇒ Shift
  }
}
sealed abstract class PortDir(val str: String, val desc: String)
case object In extends PortDir("<in>", "Port In")
case object Out extends PortDir("<out>", "Port Out")
case object Shift extends PortDir("<shift>", "Port Shift")
case class PortDef(
    name: Name,
    typeName: Name,
    dir: PortDir,
    inPos: Point,
    extPos: Point) extends SymbolTree[PortSymbol] with Positionable {
  def pos = inPos
  def changeType(tpe: String): MapTransformer = {
    new EditTransformer() {
      val trans: TreePF = {
        case p: PortDef if (p == PortDef.this) ⇒ p.copy(typeName = Name(tpe))
      }
    }
  }
  def renamePort(str: String, tpe: Option[Name]): MapTransformer = {
    val newName = if (str == name.str) name else Name(sym.portsSymbol.asInstanceOf[BoxSymbol].freshName(str))
    new EditTransformer() {
      val trans: TreePF = {
        case p: PortDef if (p == PortDef.this) ⇒
          p.copy(name = newName, typeName = tpe.getOrElse(p.typeName))
        case pr: PortRef ⇒
          val portSymbol = pr.sym.pi.portSymbol
          if (portSymbol == Some(sym)) {
            pr.copy(fromRef = transform(pr.fromRef), newName, pr.in)
          } else {
            pr
          }
      }
    }
  }
}
case class ValRef(name: Name) extends SymbolTree[ValSymbol]
case class ThisRef() extends SymbolTree[TemplateSymbol]
trait ConnectionEnd extends Tree
case class PortRef(
  fromRef: Tree,
  name: Name,
  in: Boolean) extends SymbolTree[PortSide] with ConnectionEnd // in as flow or as PortDir?
object Param {
  def apply(key: Name, value: String): Param = Param(key, List(value))
}
case class Param(key: Name, values: List[String]) extends SymbolTree[ParamDecl] {
  def valueStr = values.mkString(", ")
}

case class LabelDesc(description: String, pos: Vector2)
object ValDef {
  def emptyValStaticInvokeExpr(name: Name, dst: Point, className: String, methodUID: String) = {
    val typeP = Param(InvokeStaticExprType.typeSymbol.fqName, className)
    val methodP = Param(InvokeStaticExprType.signatureSymbol.fqName, methodUID)
    ValDef(name, InvokeStaticExprType.fqName, dst, None, List(typeP, methodP), None, None, None)
  }
  def emptyValDefBoxExpr(name: Name,
                         dst: Point,
                         className: String,
                         method: Option[String] = None,
                         label: Option[String] = None,
                         labelGui: Option[String] = None,
                         fields: Option[List[String]] = None,
                         extraParams: List[Param] = List()) = {
    val p = Param(BoxExprType.typeSymbol.fqName, className)
    val m = method.map(Param(BoxExprType.signatureSymbol.fqName, _))
    val f = fields.map(Param(BoxExprType.fieldsDecl.fqName, _))
    val lbl = label.map { LabelDesc(_, Vector2(0, 0)) }
    val lblGui = labelGui.map { LabelDesc(_, Vector2(0, 0)) }
    val params = (p :: extraParams) ++ m ++ f
    ValDef(name, BoxExprType.fqName, dst, None, params, lbl, lblGui, None)
  }
  def emptyValDef(name: Name, tpeName: Name, dst: Point) =
    ValDef(name, tpeName, dst, None, List(), None, None, None)

  def emptyValDef(name: Name, tpeName: Name, dst: Point, label: String) =
    ValDef(name, tpeName, dst, None, List(), Some(LabelDesc(label, Vector2(0, 0))), None, None)

  def emptyValDef(name: Name, tpeName: Name) =
    ValDef(name, tpeName, Point(0, 0), None, List(), None, None, None)
}
case class ValDef(
    name: Name,
    typeName: Name,
    pos: Point,
    size: Option[Dimension],
    params: List[Param],
    label: Option[LabelDesc],
    labelGui: Option[LabelDesc],
    template: Option[Template]) extends SymbolTree[ValSymbol] with Positionable {
  def transformThis(body: EditTransformer ⇒ ValDef) = new EditTransformer() {
    val trans: TreePF = {
      case v: ValDef if v == ValDef.this ⇒ body(this)
    }
  }
  def changeType(str: String) = transformThis { e ⇒
    copy(typeName = Name(str),
      template = e.transformOption(template),
      params = e.transformTrees(params))
  }
  def replaceParams(newparams: List[Param]) = transformThis { e ⇒
    copy(template = e.transformOption(template), params = newparams)
  }
  def removeParams(keys: Name*) = transformThis { e ⇒
    val filtered = params.filterNot(p ⇒ keys.contains(p.key))
    copy(template = e.transformOption(template), params = filtered)
  }
  def addOrReplaceParams(changeParams: List[Param]) = transformThis { e ⇒
    val filtered = params.filterNot(p ⇒ changeParams.exists(_.key == p.key))
    copy(
      template = e.transformOption(template),
      params = changeParams ::: filtered)
  }
  def moveLabel(gui: Boolean, newPos: Vector2) = transformThis { e ⇒
    if (gui)
      copy(labelGui = Some(labelGui.get.copy(pos = newPos)),
        template = e.transformOption(template))
    else
      copy(label = Some(label.get.copy(pos = newPos)),
        template = e.transformOption(template))
  }
  def addOrReplaceParam(param: Param) = addOrReplaceParams(List(param))
  def editType(clazz: Name) = transformThis { e ⇒
    copy(typeName = clazz, template = e.transformOption(template))
  }
  private def createLabel(gui: Boolean, s: String) = {
    val lbl = if (gui) labelGui else label
    val pos = lbl.map { _.pos }.getOrElse(Vector2(0, 0))
    if (s != "") Some(LabelDesc(s, pos)) else None
  }
  def createLabelAndRename(gui: Boolean, s: String) = {
    val v = this
    val bl = v.sym.owner
    val base = Namer.toIdentifierBase(s).getOrElse(v.name.str)
    val id: Name = if (base != v.name) Name(bl.freshName(base)) else v.name
    v.sym.owner.rename(v, id, createLabel(gui, s), gui)
  }
  def editLabel(gui: Boolean, s: String) = {
    val lbl = createLabel(gui, s)
    transformThis { e ⇒
      copy(
        template = e.transformOption(template),
        label = if (gui) this.label else lbl,
        labelGui = if (gui) lbl else this.labelGui)
    }
  }
  def bounds: Option[(Point, Dimension)] = {
    params.find(_.key == Name("bounds")).flatMap { par ⇒
      val v = RectangleValueType.create(par.values.mkString)
      if (v.valid) {
        val r = v.parse
        Some(Point(r.x, r.y), Dimension(r.width, r.height))
      } else None
    }
  }
  def updatedBounds(p: Point, d: Dimension) = {
    val strPos = p.x + " " + p.y + " " + d.w + " " + d.h
    val param = Param(Name("bounds"), strPos)
    param :: params.filterNot(_.key == param.key)
  }
}
trait TypedTree extends Tree {
  var tpe: Option[JavaType] = None
  override def clean() {
    super.clean()
    tpe = None
  }
}
case class ConnectionDef(
    a: Option[ConnectionEnd],
    b: Option[ConnectionEnd],
    points: List[Point]) extends SymbolTree[BlockSymbol] with TypedTree {
  def headPoint = points.headOption.getOrElse(Point(0, 0))
  def lastPoint = points.lastOption.getOrElse(Point(0, 0))

}
case class Junction(name: Name, p: Point) extends TypedTree
case class JunctionRef(name: Name) extends ConnectionEnd
