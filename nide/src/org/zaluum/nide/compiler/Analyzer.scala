package org.zaluum.nide.compiler

import javax.swing.JPanel
import org.jgrapht.traverse.TopologicalOrderIterator
import org.jgrapht.experimental.dag.DirectedAcyclicGraph.CycleFoundException
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.experimental.dag.DirectedAcyclicGraph
import scala.collection.mutable.Buffer

/*   def isValidJavaIdentifier(s: String) = {
    s != null &&
      s.length != 0 &&
      Character.isJavaIdentifierStart(s(0)) &&
      s.view(1, s.length).forall { Character.isJavaIdentifierPart(_) }
  }
  */

class CompilationException extends Exception
class Reporter {
  case class Error(msg: String, mark: Option[Location])
  val errors = Buffer[Error]()
  def report(str: String, mark: Option[Location] = None) {
    errors += Error(str, mark)
  }
  def check() {
    if (!errors.isEmpty)
      fail
  }
  def fail = throw new CompilationException()

  def fail(err: String, mark: Option[Location] = None): Nothing = {
    report(err)
    fail
  }
  def apply(assertion: Boolean, res: ⇒ String, mark: Option[Location] = None, fail: Boolean = false) {
    if (!assertion) report(res) // TODO mark
    if (fail) check()
  }
  def apply() = check()
  override def toString = errors.toString
}

case class Name(str: String) {
  def classNameWithoutPackage = str.split('.').last
  def toRelativePath: String = str.replace('.', '/')
  def toRelativePathClass = toRelativePath + ".class"
  def internal = str.replace('.', '/')
}
trait Scope {
  def lookupPort(name: Name): Option[Symbol]
  def lookupVal(name: Name): Option[Symbol]
  def lookupType(name: Name): Option[Type]
  def lookupBoxType(name: Name): Option[Type]
  def lookupBoxTypeLocal(name: Name): Option[Type]
  def enter[S <: Symbol](sym: S): S
  def root: Symbol
}

class FakeGlobalScope(realGlobal: Scope) extends LocalScope(realGlobal) { // for presentation compiler
  case object fakeRoot extends Symbol {
    val owner = NoSymbol
    scope = FakeGlobalScope.this
    override val name = null
  }
  override val root = fakeRoot
}
class LocalScope(val enclosingScope: Scope) extends Scope with Namer {
  protected var ports = Map[Name, Symbol]()
  protected var vals = Map[Name, Symbol]()
  protected var boxes = Map[Name, Type]()
  def lookupPort(name: Name): Option[Symbol] = ports.get(name)
  def lookupVal(name: Name): Option[Symbol] = vals.get(name)
  def lookupType(name: Name): Option[Type] = enclosingScope.lookupType(name)
  def lookupBoxType(name: Name): Option[Type] =
    boxes.get(name) orElse { enclosingScope.lookupBoxType(name) }
  def lookupBoxTypeLocal(name: Name): Option[Type] = boxes.get(name)
  def enter[S <: Symbol](sym: S): S = {
    val entry = (sym.name -> sym)
    sym match {
      case p: IOSymbol ⇒ ports += entry
      case b: BoxTypeSymbol ⇒ boxes += (sym.name -> b)
      case v: ValSymbol ⇒ vals += entry
    }
    sym
  }
  def usedNames = (boxes.keySet.map { _.str } ++ vals.keySet.map { _.str } ++ ports.keySet.map { _.str }).toSet
  def root = enclosingScope.root
}
trait ReporterAdapter {
  def location(tree: Tree): Location
  def reporter: Reporter
  def error(str: String, tree: Tree) = reporter.report(str, Some(location(tree)))
}
class Analyzer(val reporter: Reporter, val toCompile: BoxDef, val global: Scope) {
  def globLocation(t: Tree) = Location(toCompile.pathOf(t).getOrElse(throw new Exception("error")))
  class Namer(initOwner: Symbol) extends Traverser(initOwner) with ReporterAdapter {
    def reporter = Analyzer.this.reporter
    def location(tree: Tree) = globLocation(tree)
    def defineBox(symbol: BoxTypeSymbol, tree: Tree): BoxTypeSymbol =
      define(symbol, currentScope, currentScope.lookupBoxType(symbol.name).isDefined, tree)
    def defineVal(symbol: Symbol, tree: Tree): Symbol =
      define(symbol, currentScope, currentScope.lookupVal(symbol.name).isDefined, tree)
    def definePort(symbol: Symbol, tree: Tree): Symbol =
      define(symbol, currentScope, currentScope.lookupPort(symbol.name).isDefined, tree)
    def define[S <: Symbol](symbol: S, scope: Scope, dupl: Boolean, tree: Tree): S = {
      if (dupl) error("Duplicate symbol " + symbol.name, tree)
      symbol.scope = scope
      tree.scope = scope
      tree.symbol = symbol
      symbol.decl = tree
      scope enter symbol
    }
    override def traverse(tree: Tree) {
      tree match {
        case BoxDef(name, superName, guiSize, image, defs, vals, ports, connections, junctions) ⇒
          val cl = Some(Name(classOf[JPanel].getName))
          val newSym = new BoxTypeSymbol(currentOwner, name, superName, image, cl)
          val sym = defineBox(newSym, tree)
          tree.tpe = sym
          superName foreach { sn ⇒
            currentScope.lookupBoxType(sn) match {
              case Some(bs: BoxTypeSymbol) ⇒
                sym.superSymbol = Some(bs)
              case None ⇒
                error("Super box type not found " + sn, tree)
            }
          }
        case p @ PortDef(name, typeName, dir, inPos, extPos) ⇒
          definePort(new PortSymbol(currentOwner.asInstanceOf[BoxTypeSymbol], name, extPos, dir), tree) // owner of a port is boxtypesymbol
        case v @ ValDef(name, typeName, pos, size, guiPos, guiSize, params) ⇒
          defineVal(new ValSymbol(currentOwner, name), tree)
        case _ ⇒
          tree.scope = currentScope
      }
      super.traverse(tree)
    }
  }
  class Resolver(global: Symbol) extends Traverser(global) with ReporterAdapter {
    def reporter = Analyzer.this.reporter
    def location(tree: Tree) = globLocation(tree)
    override def traverse(tree: Tree) {
      super.traverse(tree)
      tree match {
        case PortDef(name, typeName, in, inPos, extPos) ⇒
          tree.symbol.tpe = currentScope.lookupType(typeName) getOrElse {
            error("Port type not found " + typeName, tree); NoSymbol
          }
          tree.tpe = tree.symbol.tpe
        case v @ ValDef(name, typeName, pos, size, guiPos, guiSize, params) ⇒
          tree.symbol.tpe = currentScope.lookupBoxType(typeName) getOrElse {
            error("Box class " + typeName + " not found", tree); NoSymbol
          }
          tree.tpe = tree.symbol.tpe
        case ValRef(name) ⇒
          tree.symbol = currentScope.lookupVal(name) getOrElse {
            error("Box not found " + name, tree); NoSymbol
          }
          tree.tpe = tree.symbol.tpe
        case p @ PortRef(fromTree, name, in) ⇒ // TODO filter in?
          tree.symbol = fromTree.tpe match {
            case b: BoxTypeSymbol ⇒
              b.lookupPort(name).getOrElse {
                error("Port not found " + name + " in box type " + b, tree);
                NoSymbol
              }
            case tpe ⇒ NoSymbol
          }
          tree.tpe = tree.symbol.tpe
        case ThisRef() ⇒
          tree.symbol = currentOwner // TODO what symbol for this?
          tree.tpe = currentOwner.asInstanceOf[BoxTypeSymbol] // owner of thisRef is owner of connection which is boxTypeSymbol
        case _ ⇒
      }
    }
  }
  class CheckParams(global: Symbol) extends Traverser(global) with ReporterAdapter {
    def reporter = Analyzer.this.reporter
    def location(tree: Tree) = globLocation(tree)
    def parseValue(p: Param, parSymbol: ParamSymbol, valSymbol: ValSymbol) {
      p.symbol = parSymbol
      p.tpe = parSymbol.tpe
      try {
        parSymbol.tpe.name match {
          case Name("double") ⇒ valSymbol.params += (parSymbol -> p.value.toDouble)
          case n ⇒ error("error type " + n.str + " cannot parse literals", p)
        }
      } catch {
        case e ⇒ error("invalid literal: " + p.value, p)
      }
    }
    override def traverse(tree: Tree) {
      super.traverse(tree)
      tree match {
        case p @ Param(name, value) ⇒
          val valSym = currentOwner.asInstanceOf[ValSymbol] // Params are from valSymbols
          valSym.tpe match {
            case b: BoxTypeSymbol ⇒
              b.params.find(_.name == name) match {
                case Some(parSymbol) ⇒ parseValue(p, parSymbol, valSym)
                case None ⇒ error("Parameter " + name + " does not exist", p)
              }
            case _ ⇒ error("cannot find type of valDef owner " + p, p) // already failed
          }
        case _ ⇒
      }
    }
  }

  class CheckConnections(b: BoxDef, owner: Symbol) {
    val bs = b.symbol.asInstanceOf[BoxTypeSymbol] // boxDefs always have boxtypesymbol
    val acyclic = new DirectedAcyclicGraph[ValSymbol, DefaultEdge](classOf[DefaultEdge])
    var usedInputs = Set[PortKey]()
    def check() = Checker.traverse(b)
    object Checker extends Traverser(owner) with ConnectionHelper with ReporterAdapter {
      def location(tree: Tree) = globLocation(tree)
      def reporter = Analyzer.this.reporter
      override def traverse(tree: Tree) {
        tree match {
          case b: BoxDef ⇒
            traverseTrees(b.vals)
            traverseTrees(b.junctions)
            traverseTrees(b.connections)
            b.defs foreach {
              case bDef: BoxDef ⇒
                new CheckConnections(bDef, b.symbol).check()
            }
            check()
          case v: ValDef ⇒ acyclic.addVertex(v.symbol.asInstanceOf[ValSymbol]) // valdefs always have symbol
          case j @ Junction(name, _) ⇒
            bs.connections.lookupJunction(name) match {
              case Some(j) ⇒ error("junction name already exists", j)
              case None ⇒ bs.connections.junctions += j
            }
          case c @ ConnectionDef(a, b, waypoints) ⇒
            if (a == EmptyTree || b.symbol == EmptyTree) {
              error("incomplete connection " + a + "<->" + b, tree)
            } else {
              bs.connections.addConnection(c)
            }
          case _ ⇒
        }
      }
      private def check() {
        def checkClump(c: Clump) {
          val ins = c.ports.filter(p ⇒ isIn(p, bs))
          val outs = c.ports.filter(p ⇒ !isIn(p, bs))
          if (outs.size == 0) error("No output connected", c.connections.head)
          else if (outs.size > 1) error("More than one output is connected", c.connections.head)
          else if (ins.size == 0) error("No inputs connected", c.connections.head)
          else {
            if (!usedInputs.intersect(ins).isEmpty) error("input connected multiple times", c.connections.head) // TODO check online to identify offending connection 
            usedInputs ++= ins
            // check types
            val types = for (pk ← c.ports; pks ← pk.resolve(bs)) yield pks.port.tpe
            if (types.size != 1) error("Connection with incompatible types " + types.mkString(","), c.connections.head)
            else {
              c.connections foreach { _.tpe = types.head }
              c.junctions foreach { _.tpe = types.head }
            }
            // check graph consistency
            val out = outs.head
            bs.connections.flow += (out -> ins)
            def addDag(vout: ValPortKeySym, vin: ValPortKeySym) {
              try {
                acyclic.addDagEdge(vout.valSym, vin.valSym);
              } catch {
                case e: CycleFoundException ⇒ error("cycle found ", c.connections.head)
                case e: IllegalArgumentException ⇒ error("loop found", c.connections.head)
              }
            }
            import org.zaluum.nide.RichCast._
            for {
              voutg ← out.resolve(bs);
              vout ← voutg.castOption[ValPortKeySym];
              pkin ← ins;
              ving ← pkin.resolve(bs);
              vin ← ving.castOption[ValPortKeySym]
            } (addDag(vout, vin))
          }
        }
        b.connections.foreach {
          case con: ConnectionDef ⇒
            def checkResolved(p: Tree) = p match {
              case EmptyTree ⇒ error("Wire is not connected", con)
              case j: JunctionRef ⇒ /*if(!bs.junctions.exists {_.name == j.name}) {
                  error("FATAL: junction does not exists " + j,con)
                }*/
              case p: PortRef ⇒
                PortKey.create(p).resolve(bs) match {
                  case None ⇒ error("Cannot find port " + p, con)
                  case _ ⇒
                }
            }
            checkResolved(con.a)
            checkResolved(con.b)
        }
        bs.connections.clumps foreach { checkClump(_) }
        import scala.collection.JavaConversions._
        val topo = new TopologicalOrderIterator(acyclic);
        bs.executionOrder = topo.toList
      }
    }
  }
  def compile(): Tree = {
    val root = global.root
    new Namer(root).traverse(toCompile)
    new Resolver(root).traverse(toCompile)
    new CheckParams(root).traverse(toCompile)
    new CheckConnections(toCompile, root).check()
    toCompile
  }
}
trait ConnectionHelper extends ReporterAdapter {
  implicit def reporter: Reporter
  def isIn(ap: PortKey, bs: BoxTypeSymbol): Boolean = {
    val resolved = ap.resolve(bs)
    println("resolved " + ap + " to " + resolved)
    resolved.map { r ⇒
      (r.port.dir, r) match {
        case (In, v: ValPortKeySym) ⇒ true
        case (In, b: BoxPortKeySym) ⇒ false
        case (Out, v: ValPortKeySym) ⇒ false
        case (Out, b: BoxPortKeySym) ⇒ true
        case (Shift, v: ValPortKeySym) ⇒ r.port.decl.asInstanceOf[PortRef].in // FIXME super ports don't have decl
        case (Shift, b: BoxPortKeySym) ⇒ r.port.decl.asInstanceOf[PortRef].in
      }
    }.getOrElse { println("true"); true }
  }
}