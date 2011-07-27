package org.zaluum.nide.zge

import org.zaluum.nide.zge.dialogs._
import org.eclipse.draw2d.RectangleFigure
import org.eclipse.draw2d.ColorConstants
import org.eclipse.draw2d.Label
import org.zaluum.nide.compiler.NoSymbol
import org.eclipse.swt.SWT
import org.eclipse.swt.widgets.ToolTip
import draw2dConversions._
import org.eclipse.draw2d.{ Cursors, Figure }
import org.eclipse.draw2d.geometry.{ Point ⇒ EPoint, Rectangle, Dimension ⇒ EDimension }
import org.zaluum.nide.compiler.{ _ }
import scala.collection.JavaConversions._
import org.zaluum.basic.LoopBox
import org.zaluum.nide.eclipse.BoxTypeProxy
import org.eclipse.draw2d.MarginBorder

class TreeTool(val viewer: TreeViewer) extends ItemTool(viewer) with ConnectionsTool {
  def tree = viewer.tree
  def zproject = viewer.controller.zproject
  val connectionLineDistance = 2
  object selecting extends Selecting with DeleteState with ClipboardState with DropState{
    var port: Option[PortFigure] = None
    override def doubleClick() {
      itemUnderMouse match {
        case Some(e: TextEditFigure) ⇒ directEditing.enter(e)
        case _ ⇒
      }
    }
    def buttonUp {
      if (filterDouble) { filterDouble = false; return }
      def selectItem(i: Item) {
        viewer.selection.updateSelection(i.selectionSubject.toSet, shift)
        viewer.refresh()
        i.selectionSubject foreach { controller.blink(_, viewer) }
      }
      (beingSelected, port) match {
        case (Some(o: OpenPortDeclFigure), _) ⇒ selectItem(o)
        case (_, Some(port)) ⇒ // connect
          portsTrack.hideTip()
          connecting.enter(port.container, port, currentMouseLocation)
        case (Some(line: LineItem), None) ⇒
          if (line.l.distance(currentMouseLocation) <= connectionLineDistance) {
            viewer.selection.updateSelection(line.selectionSubject.toSet, shift)
            viewer.refresh()
          } else {
            connecting.enter(line.container, line, currentMouseLocation)
          }
        case (Some(i: Item), None) ⇒ selectItem(i)
        case (None, _) ⇒
          viewer.selection.deselectAll()
          viewer.refresh()
        case _ ⇒
      }

    }
    val portsTrack = new PortTrack {
      override def onEnter(p: PortFigure) { super.onEnter(p); port = Some(p); viewer.setCursor(Cursors.CROSS) }
      override def onExit(p: PortFigure) { super.onExit(p); port = None }
    }
    override def move() {
      super.move()
      portsTrack.update()
      (portsTrack.current, itemUnderMouse) match {
        case (_, Some(l: OpenPortDeclFigure)) ⇒
          viewer.setCursor(Cursors.ARROW)
        case (_, Some(l: LineItem)) if (l.l.distance(currentMouseLocation) > connectionLineDistance) ⇒
          viewer.setCursor(Cursors.UPARROW)
        case (None, Some(item)) ⇒
          viewer.setCursor(Cursors.ARROW)
        case (Some(_), _) ⇒
          viewer.setCursor(Cursors.UPARROW)
        case (None, None) ⇒
          viewer.setCursor(Cursors.CROSS)
          portsTrack.current match {
            case Some(_) ⇒
            case None ⇒
          }
      }
    }
    def drag {
      portsTrack.hideTip()
      (handleTrack.current, beingSelected) match {
        case (Some(h), _) ⇒ // resize
          resizing.enter(initDrag, initContainer, h)
        case (None, Some(fig: Item)) ⇒ // select and move
          val s = fig.selectionSubject
          if (!s.isEmpty) {
            if (!viewer.selection(s.get))
              viewer.selection.updateSelection(s.toSet, false)
            fig.showFeedback()
            fig match {
              case oPort: OpenPortDeclFigure ⇒ movingOpenPort.enter(initDrag, initContainer, oPort)
              case label : LabelItem => movingLabel.enter(initDrag,initContainer,label)
              case _ ⇒ moving.enter(initDrag, initContainer)
            }
          }
        case (None, _) ⇒ marqueeing.enter(initDrag, initContainer) // marquee
      }
    }
    def drop(s:String) {
      s match {
        case In.str => creatingPort.enter(In,current)
        case Out.str => creatingPort.enter(Out,current)
        case Shift.str => creatingPort.enter(Shift,current)
        case _ =>  creating.enter(Name(s),current)
      }
    }
    def delete() {
      controller.exec(Delete.deleteSelection(viewer.selectedItems, viewer.graphOf))
    }
    def cut() {
      viewer.updateClipboard
      delete
    }
    def copy() = viewer.updateClipboard
    def paste() = viewer.getClipboard foreach { c ⇒ pasting.enter(c, current) }
    
    override def menu() {
      itemUnderMouse match {
        case Some(p: PortDeclFigure) ⇒ new PortDeclPopup(viewer, p.tree).show() // TODO Dispose?
        case Some(p: OpenPortDeclFigure) ⇒ new PortDeclPopup(viewer, p.tree).show()
        case Some(o: OpenBoxFigure) ⇒ ValDefMenu.show(viewer, o)
        case Some(l: LabelItem) => ValDefMenu.show(viewer, l)
        case Some(b: ValFigure) ⇒ ValDefMenu.show(viewer, b);
        case _ ⇒ 
      }
    }
  }
  // PASTING
  abstract class Pasting extends ToolState {
    self: SingleContainer ⇒
    var feed: ItemFeedbackFigure = _
    var clipboard: Clipboard = _
    def enter(c: Clipboard, initContainer: ContainerItem) {
      enterSingle(initContainer)
      this.clipboard = c
      state = this
      feed = new ItemFeedbackFigure(current)
      feed.setInnerBounds(new Rectangle(0, 0, 48, 48)); // XXX real clipboard size
      feed.show()
      move()
    }
    def move() { feed.setInnerLocation(point(snap(currentMouseLocation))) }
    def abort { exit() }
    def drag {}
    def buttonUp = controller.exec(clipboard.pasteCommand(initContainer, snap(currentMouseLocation)))
    def buttonDown() {}
    def exit() {
      feed.hide();
      feed = null;
      selecting.enter()
    }
  }
  object pasting extends Pasting with SingleContainerAllower
  // CREATING
  abstract class Creating extends ToolState {
    self: SingleContainer ⇒
    var feed: ItemFeedbackFigure = _
    var tpeName: Name = _
    var tpe: Option[BoxTypeProxy] = None
    def enter(tpename: Name, initContainer: ContainerItem) {
      enterSingle(initContainer)
      state = this
      this.tpeName = tpename
      tpe = controller.zproject.getBoxSymbol(tpeName);
      val (img,desc) = zproject.imageFactory(tpeName);
      feed = new ItemFeedbackFigure(current)
      feed.setInnerBounds(new Rectangle(0, 0, img.getBounds.width, img.getBounds.height));
      zproject.imageFactory.destroy(desc)
      feed.show()
    }
    def move() { feed.setInnerLocation(point(snap(currentMouseLocation))) }
    def abort() { exit() }
    def drag() {}
    private def newInstance(dst: Point) = {
      new EditTransformer() {
        val trans: PartialFunction[Tree, Tree] = {
          case b: BoxDef if b == initContainer.boxDef ⇒
            val name = Name(b.sym.freshName("box"))
            b.copy(
              defs=transformTrees(b.defs),
              vals=ValDef(name, tpeName, dst, None, None, None, List(), List(), List(),None, None) :: transformTrees(b.vals),
              ports=transformTrees(b.ports),
              connections=transformTrees(b.connections),
              junctions=transformTrees(b.junctions))
        }
      }
    }
    private def newClass(dst: Point) = {
      new EditTransformer() {
        val trans: PartialFunction[Tree, Tree] = {
          case b: BoxDef if b == initContainer.boxDef ⇒
            val sym = b.sym
            val name = Name(sym.freshName("box"))
            val className = Name(sym.freshName("C"))
            val newDef = BoxDef(className, b.pkg, Some(tpeName), guiSize = None, image = None, List(),
              vals = List(),
              ports = List(),
              connections = List(),
              junctions = List())
            val newVal = ValDef(name, className, dst, Some(Dimension(200, 200)), None, None, List(), List(), List(),None,None)
            b.copy(
              defs = newDef :: transformTrees(b.defs),
              vals = newVal :: transformTrees(b.vals),
              ports = transformTrees(b.ports),
              connections = transformTrees(b.connections),
              junctions = transformTrees(b.junctions))
        }
      }
    }
    def buttonUp() {
      val dst = snap(currentMouseLocation)
      val command = tpe match {
        case Some(b) if b.abstractCl ⇒ newClass(dst)
        case _ ⇒ newInstance(dst)
      }
      controller.exec(command)
    }
    def buttonDown() {}
    def exit() {
      feed.hide();
      feed = null;
      selecting.enter()
    }
  }
  // CREATING BOX 
  object creating extends Creating with SingleContainerAllower
  // CREATING PORT
  class CreatingPort extends ToolState {
    self: SingleContainer ⇒
    def enter(dir: PortDir, initContainer: ContainerItem) {
      enterSingle(initContainer)
      state = this
      this.dir = dir
      val (img,desc) = zproject.imageFactory.portImg(dir)
      feed = new ItemFeedbackFigure(current)
      feed.setInnerBounds(new Rectangle(0, 0, img.getBounds.width, img.getBounds.height));
      zproject.imageFactory.destroy(desc)
      feed.show()
    }
    var feed: ItemFeedbackFigure = _
    var dir: PortDir = In
    def move() { feed.setInnerLocation(point(snap(currentMouseLocation))) }
    def abort() { exit() }
    def drag() {}
    def buttonUp() {
      // execute
      val pos = snap(currentMouseLocation)
      val tr = new EditTransformer() {
        val trans: PartialFunction[Tree, Tree] = {
          case b: BoxDef if b == initContainer.boxDef ⇒
            val tpe = b.sym
            val name = Name(tpe.freshName("port"))
            val p = PortDef(name, Name("double"), dir, pos, Point(0, pos.y))
            b.copy(
              defs=transformTrees(b.defs),
              vals=transformTrees(b.vals),
              ports=p :: transformTrees(b.ports),
              connections=transformTrees(b.connections),
              junctions=transformTrees(b.junctions))
        }
      }
      controller.exec(tr)
    }
    def buttonDown() {}
    def exit() { feed.hide(); feed = null; selecting.enter() }
  }
  object creatingPort extends CreatingPort with SingleContainerAllower
  
  trait MovingLabel extends SpecialMove[LabelItem]{
    self : ToolState with DeltaMove with SingleContainer =>
    def clampDelta = delta
    def buttonUp {
      val oldPos = fig.valDef.label.get.pos
      val newPos = oldPos + clampDelta
      val command = new EditTransformer {
        val trans: PartialFunction[Tree, Tree] = {
          case v: ValDef if (fig.valDef == v) ⇒
            v.copy(label = Some(v.label.get.copy(pos=newPos)))
        }
      }
      controller.exec(command)
    }
  }
  object movingLabel extends MovingLabel with DeltaMove with SingleContainer
  // MOVING OPEN PORT
  trait MovingOpenPort extends SpecialMove[OpenPortDeclFigure]{
    self:  ToolState with DeltaMove with SingleContainer =>
    def minY = 0
    def maxY = fig.openBox.size.h
    def posY = fig.relPos.y
    def minDelta = minY - posY
    def maxDelta = maxY - posY - fig.size.h
    def clamp(low: Int, i: Int, high: Int) = math.max(low, math.min(i, high))
    def clampDelta = Vector2(0, clamp(minDelta, delta.y, maxDelta))
    def buttonUp {
      val oldPos = fig.tree.extPos
      val newPos = oldPos + clampDelta
      val command = new EditTransformer {
        val trans: PartialFunction[Tree, Tree] = {
          case p: PortDef if (fig.tree == p) ⇒
            p.copy(extPos = newPos)
        }
      }
      controller.exec(command)
    }
  }
  object movingOpenPort extends MovingOpenPort with DeltaMove with SingleContainer
  
  class PortTrack extends OverTrack[PortFigure] {
    class TooltipLabel extends RectangleFigure {
      val l = new Label
      l.setBorder(new MarginBorder(4,4,4,4));
      l.setFont(viewer.display.getSystemFont)
      setBackgroundColor(ColorConstants.tooltipBackground)
      setForegroundColor(ColorConstants.tooltipForeground)
      l.setForegroundColor(ColorConstants.tooltipForeground)
      add(l)
      def setText(s: String) { l.setText(s) }
      override def getPreferredSize(x: Int, y: Int) = l.getPreferredSize(x, y)
      override def setSize(x: Int, y: Int) = { l.setSize(x, y); super.setSize(x, y) }
    }
    lazy val tooltip = new TooltipLabel
    def showTip(pf: PortFigure) {
      val abs = pf.anchor.getCopy
      val name = if (pf.nameOverride!="") pf.nameOverride else pf.ps.name.str 
      val text = name + " : " + pf.ps.pi.finalTpe.name.str  
      tooltip.setText(text)
      viewer.feedbackLayer.add(tooltip)
    }
    override def update {
      super.update
      tooltip.setLocation(draw2dConversions.point(absMouseLocation + Vector2(15, 15)))
      tooltip.setSize(tooltip.getPreferredSize())
    }
    def hideTip() {
      if (viewer.feedbackLayer.getChildren.contains(tooltip))
        viewer.feedbackLayer.remove(tooltip)
    }
    override def onEnter(p: PortFigure) { super.onEnter(p); p.hover = true; showTip(p) }
    override def onExit(p: PortFigure) { super.onExit(p); p.hover = false; hideTip }
  }
}
