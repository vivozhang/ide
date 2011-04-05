package org.zaluum.nide.zge

import org.eclipse.draw2d.Shape
import org.eclipse.swt.graphics.Color
import org.eclipse.draw2d.Ellipse
import org.eclipse.swt.events.FocusListener
import org.eclipse.jface.viewers.ICellEditorListener
import org.eclipse.swt.widgets.Text
import org.eclipse.jface.viewers.TextCellEditor
import org.eclipse.draw2d.text.TextFlow
import org.eclipse.draw2d.text.FlowPage
import org.eclipse.draw2d.RectangleFigure
import draw2dConversions._
import org.eclipse.draw2d.{ ColorConstants, Figure, ImageFigure, Polyline, Graphics }
import org.eclipse.draw2d.geometry.{ Rectangle, Point ⇒ EPoint, Dimension ⇒ EDimension }
import org.eclipse.swt.SWT
import org.eclipse.swt.graphics.Image
import org.zaluum.nide.compiler._
import scala.collection.mutable.Buffer
import org.eclipse.swt.widgets.{ Composite, Display, Shell, Listener, Event }
import javax.swing.JButton
import javax.swing.JComponent
import java.awt.{ Graphics ⇒ AG }
import java.awt.image.BufferedImage

// TREE SPECIFIC FIGURES
trait ValDefItem extends Item {
  var valDef : ValDef = _
  def pos = valDef.pos
  override def selectionSubject = Some(valDef)
  def updateValDef(t:ValDef) {
    valDef = t
    updateMe()
    updateSize()
    updateValPorts()
  }
  def updateMe()
  def updateValPorts()
}

trait ValFigure extends ValDefItem with HasPorts{
  def sym = valDef.symbol.asInstanceOf[ValSymbol]
  def myLayer = container.layer
  def updateValPorts() {
    for (p<-ports) container.portsLayer.remove(p)
    ports.clear
    sym.tpe match {
      case b: BoxTypeSymbol ⇒
        b.ports.values.foreach {
          case s: PortSymbol ⇒
            val p = new PortFigure(container)
            p.update(s.extPos + Vector2(getBounds.x, getBounds.y),s, sym, s.dir == In)
            ports += p
          case _ =>
        }
      case _ ⇒ List()
    }
  }
}
class ImageValFigure(val container: ContainerItem) extends ImageFigure with ValFigure with RectFeedback {
  def size = Dimension(getImage.getBounds.width, getImage.getBounds.height)
  def updateMe() {
    setImage(container.viewerResources.imageFactory(valDef.tpe))    
  }
}
class DirectValFigure(val container: ContainerItem) extends TextEditFigure with ValFigure {
  def size = Dimension(40, 20)
  def param = valDef.params.head.asInstanceOf[Param]
  def text = param.value
  def updateMe {
    fl.setText(text)
    setForegroundColor(Colorizer.color(param.tpe))    
  }
}
trait TextEditFigure extends RectangleFigure with Item with RectFeedback {
  def text: String;
  val pg = new FlowPage()
  pg.setForegroundColor(ColorConstants.black)
  pg.setBounds(new Rectangle(2, 2, 40, 20))
  val fl = new TextFlow()
  pg.add(fl)
  add(pg)

  var textCellEditor: TextCellEditor = null
  def edit(onComplete: (String) ⇒ Unit, onCancel: () ⇒ Unit) = {
    if (textCellEditor == null) {
      textCellEditor = new TextCellEditor(container.viewer.canvas)
      val textC = textCellEditor.getControl().asInstanceOf[Text]
      textC.setText(text)
      textCellEditor.activate()
      textCellEditor.addListener(new ICellEditorListener() {
        def applyEditorValue() { onComplete(textC.getText) }
        def cancelEditor() { onCancel() }
        def editorValueChanged(oldValid: Boolean, newValid: Boolean) {}
      })
      val b = getClientArea.getCopy
      translateToAbsolute(b)
      textC.setBounds(b.x + 1, b.y + 1, b.width - 2, b.height - 2)
      textC.setBackground(ColorConstants.white)
      textC.setVisible(true)
      textC.selectAll()
      textC.setFocus
    }
  }
  def hideEdit() = {
    if (textCellEditor != null) {
      textCellEditor.dispose()
      textCellEditor = null
    }
  }
}
class SwingFigure(val container: ContainerItem, val valDef: ValDef, val component: JComponent) extends Figure with Item with ResizableFeedback {
  setOpaque(true)
  def size = valDef.guiSize getOrElse { Dimension(15, 15) }
  def pos = valDef.guiPos getOrElse { Point(0, 0) }
  def myLayer = container.layer
  def updatePorts {}
  def updateMain {}
  def helpers = List()
  override def paintFigure(g: Graphics) {
    val rect = getClientArea()
    component.setBounds(0, 0, rect.width, rect.height);
    component.doLayout
    val aimage = new BufferedImage(rect.width, rect.height, BufferedImage.TYPE_INT_RGB)
    val ag = aimage.createGraphics
    component.paint(ag)
    val imageData = SWTUtils.convertAWTImageToSWT(aimage)
    val image = new org.eclipse.swt.graphics.Image(Display.getCurrent(), imageData)
    g.drawImage(image, rect.x, rect.y)
    ag.dispose();
    image.dispose()
  }
}