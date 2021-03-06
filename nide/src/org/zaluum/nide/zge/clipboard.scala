package org.zaluum.nide.zge

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import scala.annotation.tailrec
import scala.collection.mutable.Map
import org.eclipse.swt.dnd.ByteArrayTransfer
import org.eclipse.swt.dnd.Transfer
import org.eclipse.swt.dnd.TransferData
import org.zaluum.nide.compiler.Block
import org.zaluum.nide.compiler.ConnectionDef
import org.zaluum.nide.compiler.ConnectionEnd
import org.zaluum.nide.compiler.EditTransformer
import org.zaluum.nide.compiler.MapTransformer
import org.zaluum.nide.compiler.Name
import org.zaluum.nide.compiler.Namer
import org.zaluum.nide.compiler.Parser
import org.zaluum.nide.compiler.Point
import org.zaluum.nide.compiler.PortDef
import org.zaluum.nide.compiler.PortRef
import org.zaluum.nide.compiler.SelectionSubject
import org.zaluum.nide.compiler.Serializer
import org.zaluum.nide.compiler.Template
import org.zaluum.nide.compiler.Tree
import org.zaluum.nide.compiler.ValDef
import org.zaluum.nide.compiler.ValRef
import org.zaluum.nide.compiler.Vector2
import org.zaluum.nide.protobuf.ZaluumProtobuf
import org.zaluum.nide.compiler.Param

case class Clipboard(valDefs: List[ValDef], ports: List[PortDef], connections: List[ConnectionDef]) {
  def isEmpty: Boolean = valDefs.isEmpty && ports.isEmpty
  def toTopLeftZero(gui: Boolean): Clipboard = {
    val treePositions = valDefs.view.map(_.pos) ++ ports.view.map(_.inPos)
    val guiPositions = valDefs.view.flatMap(_.bounds).map { case (p, d) ⇒ p }
    val positions = if (gui) guiPositions else treePositions
    val delta =
      if (positions.isEmpty) Vector2(0, 0)
      else
        positions.min.toVector.negate
    if (gui)
      move(identity, p ⇒ p + delta)
    else
      move(p ⇒ p + delta, identity)
  }
  def move(tree: Point ⇒ Point, gui: Point ⇒ Point): Clipboard = {
    val newVals = valDefs.map { v ⇒
      v.bounds match {
        case Some((p, d)) ⇒
          v.copy(params = v.updatedBounds(gui(p), d), pos = tree(v.pos))
        case None ⇒
          v.copy(pos = tree(v.pos))
      }
    }
    val newPorts = ports.map { p ⇒
      val e = tree(p.extPos)
      p.copy(
        inPos = tree(p.inPos),
        extPos = e.copy(x = 0, y = math.max(0, e.y)))
    }
    val newCons = connections.map { c ⇒ c.copy(points = c.points map tree) }
    Clipboard(newVals, newPorts, newCons)
  }
  def relocate(delta: Vector2, container: ContainerItem, gui: Boolean): Clipboard = {
      def treeTranslate(p: Point): Point = rpoint(container.translateMineFromViewport_!(point(p + delta)))
      def guiTranslate(p: Point) = p + delta
    if (gui)
      move(identity, guiTranslate)
    else
      move(treeTranslate, identity)
  }
  def rename(baseNamer: Namer): Clipboard = {
    val newNames = Map[String, String]()
    val namer = new Namer() {
      def usedNames = baseNamer.usedNames ++ newNames.values
    }
      def rename(oldName: String): String = {
        val newName = namer.freshName(oldName)
        newNames += (oldName -> newName)
        newName
      }

    val newVals = valDefs map { v ⇒
      v.copy(name = Name(rename(v.name.str)))
    }
    val newPorts = ports map { p ⇒
      p.copy(name = Name(rename(p.name.str)))
    }
    val newConn = connections map { c ⇒
        def renameRef(t: Option[ConnectionEnd]) = t match {
          case Some(p: PortRef) ⇒
            val vr = p.fromRef.asInstanceOf[ValRef]
            Some(p.copy(fromRef = vr.copy(name = Name(newNames(vr.name.str)))))
          case a ⇒ a
        }
      c.copy(
        a = renameRef(c.a),
        b = renameRef(c.b))
    }
    Clipboard(newVals, newPorts, newConn)
  }
  def renameRelocate(baseNamer: Namer, delta: Vector2, container: ContainerItem, gui: Boolean): Clipboard = {
    relocate(delta, container, gui).rename(container.block.sym)
  }
  def pasteCommand(c: ContainerItem, absPos: Point, gui: Boolean): MapTransformer = {
    val delta = absPos.toVector
    val zero = toTopLeftZero(gui)
    val renamed: Clipboard = zero.renameRelocate(c.block.sym, delta, c, gui)
    new EditTransformer() {
      val trans: PartialFunction[Tree, Tree] = {
        case t: Template if t == c.template ⇒
          val tpe = t.sym
          t.copy(blocks = transformTrees(t.blocks),
            ports = transformTrees(t.ports) ++ renamed.ports)
        case bl: Block if bl == c.block ⇒
          bl.copy(valDefs = transformTrees(bl.valDefs) ++ renamed.valDefs,
            connections = transformTrees(bl.connections) ++ renamed.connections,
            junctions = transformTrees(bl.junctions))
      }
    }
  }
}

object Clipboard {
  /** selection must not contain nested selections */
  def createFromSelection(selection: Set[Item]): Clipboard = {
      // fixme shift ports duplicated
      def findItemOf(t: Tree): Item = {
        selection.find(i ⇒
          i.selectionSubject.exists(_.selectedTree == t)).get
      }
    val trees = for (i ← selection; s ← i.selectionSubject) yield { s.selectedTree }
      def translate(p: Point, t: Tree) = findItemOf(t).container.translateMineToViewport_!(point(p))
    val movedTrees = for (t ← trees) yield {
      t match {
        case v: ValDef  ⇒ v.copy(pos = translate(v.pos, v))
        case p: PortDef ⇒ p.copy(inPos = translate(p.inPos, p), extPos = translate(p.extPos, p))
        case c: ConnectionDef ⇒
          val i = findItemOf(c)
          c.copy(points = c.points map i.translateToViewport)
        case a ⇒ a
      }
    }
    val valDefs = movedTrees collect { case v: ValDef ⇒ v }
    val portDefs = movedTrees collect { case p: PortDef ⇒ p }
      // Only connections valDefs TODO improve
      def valid(c: ConnectionDef) = {
          def validEnd(o: Option[ConnectionEnd]) = o match {
            case Some(pr: PortRef) ⇒
              pr.fromRef match {
                case vr: ValRef ⇒ valDefs.exists { _.name == vr.name }
                case _          ⇒ false
              }
            case None ⇒ true
            case _    ⇒ false
          }
        validEnd(c.a) && validEnd(c.b)
      }
    val connections = movedTrees.collect { case c: ConnectionDef if (valid(c)) ⇒ c }
    Clipboard(valDefs.toList, portDefs.toList, connections.toList)
  }
}
object ClipTransfer extends ByteArrayTransfer {
  val typeName = "boxTransfer:" + System.currentTimeMillis() + ":" + ClipTransfer.hashCode
  val typeID = Transfer.registerType(typeName);
  override protected def getTypeIds = Array(typeID)
  override protected def getTypeNames = Array(typeName)

  override protected def javaToNative(data: Object, transferData: TransferData) =
    data match {
      case c: ZaluumProtobuf.Clipboard ⇒
        val b = new ByteArrayOutputStream()
        c.writeDelimitedTo(b);
        super.javaToNative(b.toByteArray, transferData)
    }

  override protected def nativeToJava(transferData: TransferData) = {
    val bytes = super.nativeToJava(transferData).asInstanceOf[Array[Byte]]
    ZaluumProtobuf.Clipboard.parseDelimitedFrom(new ByteArrayInputStream(bytes));
  }
}

trait ClipboardViewer {
  this: ItemViewer ⇒
  def updateClipboard = {
    val nclip = Clipboard.createFromSelection(selectedItems)
    if (!nclip.isEmpty) {
      val clip = Serializer.proto(nclip)
      val eclip = new org.eclipse.swt.dnd.Clipboard(display)
      eclip.setContents(Array(clip), Array(ClipTransfer))
    }
  }
  import scala.util.control.Exception._
  def getClipboard: Option[Clipboard] = try {
    val eclip = new org.eclipse.swt.dnd.Clipboard(display)
    Option(eclip.getContents(ClipTransfer).asInstanceOf[ZaluumProtobuf.Clipboard]).
      map { c ⇒ Parser.parse(c) }
  } catch { case e ⇒ None }
}