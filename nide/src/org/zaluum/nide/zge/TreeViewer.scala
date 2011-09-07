package org.zaluum.nide.zge

import scala.collection.mutable.Buffer
import org.eclipse.draw2d.geometry.Rectangle
import org.eclipse.draw2d.ColorConstants
import org.eclipse.draw2d.FreeformLayer
import org.eclipse.draw2d.Label
import org.eclipse.draw2d.ScalableFreeformLayeredPane
import org.eclipse.swt.widgets.Composite
import org.zaluum.nide.compiler.Block
import org.zaluum.nide.compiler.ConnectionDef
import org.zaluum.nide.compiler.Dimension
import org.zaluum.nide.compiler.Point
import org.zaluum.nide.compiler.PortDef
import org.zaluum.nide.compiler.SelectionSubject
import org.zaluum.nide.compiler.Tree
import org.zaluum.nide.eclipse.GraphicalEditor
import org.zaluum.nide.Activator

class TreeViewer(parent: Composite, controller: Controller, editor: GraphicalEditor)
    extends ItemViewer(parent, controller) with ClipboardViewer {
  /*MODEL*/
  def tree = controller.tree
  def boxDef = tree
  def block = boxDef.template.blocks.head
  def viewer = this
  /*LAYERS*/
  val tool: TreeTool = new TreeTool(this)
  def zproject = controller.zproject
  def gotoMarker(l: Int) {
    controller.findPath(l) foreach { t ⇒
      selection.updateSelection(Set(t), false)
      refresh()
      focus
    }
  }
  def onFocus { editor.showViews() }
  def onResize { refresh() } // FIXME this OnResize triggers when a tooltip expands the canvas
  // Viewer doesn't have any visual representation
  override def updateSize() {}
  val feed = null
  def hideme() {}
  def showme() {}
  def size = Dimension(0, 0)
  def pos = Point(0, 0)
  def container = this
  def myLayer = null
  val ports = Buffer[PortDeclFigure]()
  def updatePorts(changes: Map[Tree, Tree]) {
    ports.foreach { _.hide }
    ports.clear
    symbol.template.thisVal.portSides foreach { pside ⇒
      pside.pi.portSymbol match {
        case Some(ps) ⇒
          ps.decl match {
            case pd: PortDef ⇒
              val f = new PortDeclFigure(pd, pside, TreeViewer.this)
              f.update()
              ports += f
            case _ ⇒
          }
        case _ ⇒
      }
    }
    ports.foreach { _.show }
  }
  override def dispose() {
    super.dispose()
  }
  import RichFigure._
  def remapSelection(m: PartialFunction[SelectionSubject, SelectionSubject]) {
    val mapper = m.orElse {
      //map LineSelectionSubject
      new PartialFunction[SelectionSubject, SelectionSubject] {
        def isDefinedAt(s: SelectionSubject): Boolean = {
          s match {
            case l: LineSelectionSubject ⇒
              if (m.isDefinedAt(l.c)) {
                val cd = m(l.c).asInstanceOf[ConnectionDef]
                Edge(cd).lines exists { _ == l.l }
              } else false
            case _ ⇒ false
          }
        }
        def apply(s: SelectionSubject): SelectionSubject = {
          val l = s.asInstanceOf[LineSelectionSubject]
          val cd = m(l.c).asInstanceOf[ConnectionDef]
          LineSelectionSubject(cd, l.l)
        }
      }
    };
    selection.refresh(mapper);
  }

  val emptyLabel = new Label("Empty File. Start by dropping items from the palette...")
  emptyLabel.setForegroundColor(ColorConstants.lightGray)
  emptyLabel.setFont(Activator.getDefault.directEditFont)
  def showEmptyLabel() = {
    val x = getBounds.width / 2
    val y = getBounds.height / 2
    val d = emptyLabel.getPreferredSize();
    val dx = d.width / 2
    val dy = d.height / 2
    emptyLabel.setBounds(new Rectangle(x - dx, y - dy, d.width, d.height))
    this.feedbackLayer.add(emptyLabel)
  }
  def hideEmptyLabel() = {
    if (feedbackLayer.getChildren.contains(emptyLabel))
      feedbackLayer.remove(emptyLabel)
  }
  def deepChildrenWithoutLayers = this.deepChildren.filter {
    _ match {
      case _: FreeformLayer               ⇒ false
      case _: ScalableFreeformLayeredPane ⇒ false
      case _                              ⇒ true
    }
  }
  def refresh() {
    hideEmptyLabel()
    updateContents(Map()) // FIXME
    if (deepChildrenWithoutLayers.isEmpty) showEmptyLabel()
    selectedItems foreach { _.showFeedback() }
    editor.setSelection(selectedItems.headOption)
    /*for (s <- selectedItems; ss <- s.selectionSubject) {
      println ("selected : " + ss)
      ss match {
        case t:Tree => 
          println ("symbol: " + t.symbol)
          println ("tpe: " + t.tpe )
        case _ =>
      }
    }*/
  }
  def selectedItems = this.deepChildren.collect {
    case i: Item if i.selectionSubject.isDefined && selection(i.selectionSubject.get) ⇒ i
  }.toSet
  def graphOf(b: Block) = {
    if (block == b) Some(graph)
    else {
      this.deepChildren.view.collect {
        case c: ContainerItem ⇒ c
      } filter { _.block == b } map { _.graph } headOption
    }
  }
}
