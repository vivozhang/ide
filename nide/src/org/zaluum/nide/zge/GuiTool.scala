package org.zaluum.nide.zge

import scala.collection.JavaConversions.asScalaBuffer
import scala.math.max
import org.eclipse.draw2d.geometry.Rectangle
import org.eclipse.draw2d.Cursors
import org.eclipse.draw2d.IFigure
import org.zaluum.nide.compiler._
import org.zaluum.`object`.BoxInstance
import org.zaluum.nide.palette.PaletteEntry

class GuiTool(viewer: GuiViewer) extends ItemTool(viewer) {
  val gui = true
  val gridSize = 12
  def calcMin: Dimension = {
    val bottomRights = viewer.layer.getChildren.collect { case i: IFigure ⇒ rpoint(i.getBounds.getBottomRight) }
    maxPoint(bottomRights)
  }
  def maxPoint(points: Iterable[Point]) = points.foldLeft(Dimension(0, 0))((acc, p) ⇒ Dimension(max(acc.w, p.x), max(acc.h, p.y)))
  override val resizing = new Resizing {
    def exec(newPos: Point, newSize: Dimension) {
      itf match {
        case s: SwingFigure ⇒
          val t = s.valDef
          val newDim = Dimension(newPos.x + newSize.w, newPos.y + newSize.h)
          val strPos = newPos.x + " " + newPos.y + " " + newSize.w + " " + newSize.h
          controller.exec(EditTransformer(e ⇒ {
            case b: BoxDef if b == viewer.boxDef ⇒
              b.copy(guiSize = Some(viewer.backRect.getSize.ensureMin(newDim)),
                template = e.transform(b.template))
            case v: ValDef if (v == t) ⇒
              val param = Param(Name("bounds"), strPos)
              val filtered = v.params.filterNot(_.key == param.key)
              v.copy(
                template = e.transformOption(v.template),
                params = param :: filtered)
          }: TreePF))
        case _ ⇒ abort()
      }
    }
  }
  object selecting extends Selecting with DropState {
    var border = (false, false)
    override def buttonDown {
      border = borderDistance
      super.buttonDown
    }
    def editLabel(s: String, l: LabelItem) =
      l.valDef.editLabel(true, s)
    def buttonUp {
      beingSelected match {
        case Some(s: Item) ⇒
          viewer.selection.updateSelection(s.selectionSubject.toSet, shift)
          s.selectionSubject foreach { controller.blink(_, viewer) }
        case None ⇒ viewer.selection.deselectAll()
      }
      viewer.redraw()
    }
    def delete() {
      controller.exec(Delete.deleteSelectionAndPaste(viewer.selectedItems, viewer.editor.viewer.graphOf))
    }

    val borderSensivity = 5
    def borderDistance = {
      val br = viewer.backRect.getBounds.getBottomRight
      (math.abs(currentMouseLocation.x - br.x) < borderSensivity, math.abs(currentMouseLocation.y - br.y) < borderSensivity)
    }
    def drop(a: AnyRef) {
      a match {
        case e: PaletteEntry ⇒
          e.name match {
            case In.str    ⇒
            case Out.str   ⇒
            case Shift.str ⇒
            case _         ⇒ creating.enter(e)
          }
        case _ ⇒
      }
    }
    override def move {
      borderDistance match {
        case (true, true)  ⇒ viewer.setCursor(Cursors.SIZESE)
        case (false, true) ⇒ viewer.setCursor(Cursors.SIZES)
        case (true, false) ⇒ viewer.setCursor(Cursors.SIZEE)
        case _             ⇒ viewer.setCursor(Cursors.ARROW)
      }
      super.move
    }
    def drag {
      (handleTrack.current, beingSelected, border) match {
        case (_, _, a) if a != (false, false) ⇒
          resizingGui.enter(initDrag, a)
        case (Some(h), _, _) ⇒ // resize
          resizing.enter(initDrag, initContainer, h)
        case (None, Some(fig), _) ⇒ // select and move
          if (fig.selectionSubject.isDefined) {
            if (!viewer.selection(fig.selectionSubject.get)) {
              viewer.selection.updateSelection(fig.selectionSubject.toSet, shift)
              fig.showFeedback()
            }
          }
          fig match {
            case s: SwingFigure ⇒ moving.enter(initDrag, initContainer)
            case l: LabelItem   ⇒ movingLabel.enter(initDrag, initContainer, l)
          }
        case (None, None, _) ⇒ marqueeing.enter(initDrag, initContainer) // marquee
      }
    }
    override def menu() {}
    def cut() {
      viewer.updateClipboard
      delete
    }
    def copy() = viewer.updateClipboard
    def paste() = viewer.getClipboard foreach { c ⇒ pasting.enter(c, current) }

  }
  object pasting extends Pasting with SingleContainerAllower {
    val gui = true
  }
  object creating extends GuiCreating
  class GuiCreating extends Creating with Allower {
    val defaultSize = Dimension(40, 15)
    def allowed = entry != null && entry.tpe == BoxExprType.fqName.str
    protected def getSize(entry: PaletteEntry) = defaultSize

    protected def newInstance(dst: Point, blocks: Int) = {
      val container = viewer.treeViewer.findContainerAt(point(dst))
      val block = container match {
        case o: OpenBoxFigure ⇒ o.block
        case v: Viewer        ⇒ viewer.block
      }
      val d = container.translateFromViewport(point(dst))
      Some(new EditTransformer() {
        val trans: PartialFunction[Tree, Tree] = {
          case b: Block if b == block ⇒
            val bParam = Param(Name("bounds"),
              dst.x + " " + dst.y + " " + defaultSize.w + " " + defaultSize.h)
            val v = entry.toValDef(b, d, None, None, List(bParam))
            val newVal = v.copy(params = bParam :: v.params.filterNot(_.key == Name("bounds")))
            b.copy(
              valDefs = newVal :: transformTrees(b.valDefs),
              connections = transformTrees(b.connections),
              parameters = transformTrees(b.parameters),
              junctions = transformTrees(b.junctions))
        }
      })
    }
  }
  trait ResizingGui extends ToolState {
    self: DeltaMove ⇒
    import scala.collection.JavaConversions._
    import math.max
    var mode = (false, false)
    var initSize = Dimension(0, 0)
    def currentMouseLocation = GuiTool.this.currentMouseLocation

    def enter(initDrag: Point, mode: (Boolean, Boolean)) {
      enterMoving(initDrag)
      initSize = viewer.backRect.getSize
      this.mode = mode
      state = this
    }
    def filterDelta = Vector2(if (mode._1) delta.x else 0, if (mode._2) delta.y else 0)
    def newSize = (initSize + filterDelta).ensureMin(Dimension(10, 10))
    def move {
      viewer.backRect.setSize(dimension(newSize))
    }
    def buttonUp {
      if (newSize != initSize) {
        val command = new EditTransformer {
          val trans: PartialFunction[Tree, Tree] = {
            case b: BoxDef if (b == viewer.boxDef) ⇒
              b.copy(
                guiSize = Some(newSize),
                template = transform(b.template))
          }
        }
        controller.exec(command)
      } else {
        exit()
      }
    }
    def buttonDown {}
    def drag() {}
    def abort() {
      viewer.backRect.setSize(dimension(initSize))
      exit()
    }
    def exit() { selecting.enter() }
  }
  object resizingGui extends ResizingGui with DeltaMove

  // MOVING
  trait Moving extends ToolState {
    self: DeltaMove ⇒
    def enter(initDrag: Point, initContainer: C) {
      enterMoving(initDrag)
      state = this
    }
    def snapDelta = {
      val order = Ordering.fromLessThan[Point]((a, b) ⇒ a.x < b.x || a.y < b.y)
      val topleft = viewer.selectedItems.collect { case s: SwingFigure ⇒ s.pos }.min(order)
      snap(topleft + delta) - topleft
    }
    def buttonUp {
      val sdelta = snapDelta
      val valdefs = viewer.selectedItems.collect { case item: SwingFigure ⇒ item.valDef -> item }.toMap
      val command = new EditTransformer {
        val trans: PartialFunction[Tree, Tree] = {
          case b: BoxDef if (b == viewer.boxDef) ⇒
            b.copy(template = transform(b.template))
          case v: ValDef if (valdefs.contains(v)) ⇒
            v.copy(
              template = transformOption(v.template),
              params = v.updatedBounds(valdefs(v).pos + sdelta, valdefs(v).size))
        }
      }
      controller.exec(command)
    }
    def drag {}
    def buttonDown {}
    def exit() { selecting.enter() }
    def move() {
      val sdelta = snapDelta
      viewer.selectedItems foreach { f ⇒ f.moveFeed(f.pos + sdelta) }
    }
    def abort() {
      viewer.selectedItems foreach { f ⇒ f.moveFeed(f.pos) }
      exit()
    }
    def currentMouseLocation = GuiTool.this.currentMouseLocation
  }
  object moving extends Moving with DeltaMove

}
