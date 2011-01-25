package org.zaluum.nide.zge

import org.zaluum.nide.model.Box
import javax.swing.JButton
import javax.swing.JComponent
import java.awt.{ Graphics ⇒ AG }
import java.awt.image.BufferedImage
import org.eclipse.draw2d.{ Figure, Graphics }
import org.eclipse.swt.widgets.{ Composite, Display }
import org.zaluum.nide.model.{ Resizable, Dimension, Positionable, Point, Model}
import org.zaluum.nide.compiler.BoxClassPath
import scala.collection.mutable.Buffer



class SwingFigure(val viewer: GUIViewer, val box: Box, val component:JComponent) extends Figure with ResizableItemFigure {
  lazy val feed = new ResizeItemFeedbackFigure(this)
  def positionable = box.guiPos 
  def resizable = box.guiPos
  def size = box.guiPos.size
  setOpaque(true)
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
class GUIModelView(viewer: GUIViewer, val model: Model, val bcp:BoxClassPath) extends AbstractModelView[GUIModel](viewer) {
  object widgetMapper extends ModelViewMapper[Box, SwingFigure] {
    def guiClass(box:Box) = bcp.find(box.className) flatMap {_.guiClass}
    def modelSet = {
      model.boxes filter {guiClass(_).isDefined }
    }
    def buildFigure(guiBox: Box) = {
      val component = guiClass(guiBox) map { _.newInstance.asInstanceOf[JComponent] } getOrElse { new JButton("Not found") }
      // TODO catch exceptions 
      println ("component " + component)
      new SwingFigure(viewer, guiBox, component)
    }
  }
  def update() {
    widgetMapper.update()
  }
}
object ExampleGUI {
  def simpleModel = {
    val m= new GUIModel
   // val button = new JButton("hola")
   // m.widgets += new Widget(button, Point(50,50), Dimension(60,60))
    m
  }
}
class GUIModel
class GUIController(val model: GUIModel, val boxmodel:Model, bcp:BoxClassPath) extends AbstractController[GUIModel] {
  private var viewModels = Buffer[GUIModelView]()
  def registerView(viewer: GUIViewer) = {
    val viewModel = new GUIModelView(viewer, boxmodel,bcp)
    viewModels += viewModel
    viewModel.update()
    viewModel
  }
  def updateViewers { viewModels foreach { _.update() } }
  def abortTools() { viewModels foreach { _.viewer.tool.state.abort() } }
}
class GUIViewer(parent: Composite, controller: GUIController) extends AbstractViewer(parent, controller) {
  /*TOOLS*/
  //val palette = new Palette(this, parent.getShell)
  var tool = new GUITool(this)
  /*MODEL*/
  override val modelView = controller.registerView(this)
  def model = controller.model
}
class GUITool(val viewer: GUIViewer) extends AbstractTool(viewer) 