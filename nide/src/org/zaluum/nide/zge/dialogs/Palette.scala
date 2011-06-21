package org.zaluum.nide.zge.dialogs

import org.eclipse.swt.custom.ScrolledComposite
import org.eclipse.swt.graphics.{ Image, Point }
import org.eclipse.swt.layout.{ GridData, GridLayout }
import org.eclipse.swt.widgets.{ Shell, Composite, Button }
import org.eclipse.swt.SWT
import org.eclipse.ui.forms.events.{ ExpansionAdapter, ExpansionEvent }
import org.eclipse.ui.forms.widgets.ExpandableComposite
import org.zaluum.nide.compiler.{ In, Out, PortDir, Shift, Name }
import org.zaluum.nide.zge.SWTScala._
import org.zaluum.nide.zge._
import org.eclipse.core.runtime.jobs.Job
import org.eclipse.core.runtime.Platform
import org.eclipse.ui.PlatformUI
import org.eclipse.jface.operation.IRunnableWithProgress
import org.eclipse.core.runtime.IProgressMonitor
import org.zaluum.nide.eclipse.EclipseUtils
import org.zaluum.nide.eclipse.BoxTypeProxy

object Palette {
  val w = 400
  val h = 300
}
class Palette(viewer: TreeViewer, mainShell: Shell) extends ScrollPopup(mainShell) {
  def name = "Palette"
  def columns = 5
  var container: ContainerItem = _
  def show(container: ContainerItem) {
    this.container = container
    super.show()
  }
  def newComposite(c: Composite) = {
    val content = new Composite(c, SWT.NONE)
    val layout = new GridLayout
    layout.numColumns = columns
    layout.verticalSpacing = 10;
    layout.makeColumnsEqualWidth = true;
    content.setLayout(layout)
    content
  }
  def newBar(name: String, top: Composite) = {
    val expand = new ExpandableComposite(top, SWT.NONE, ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT);
    val content = newComposite(expand)
    expand.setClient(content)
    expand.setText(name)
    expand.addExpansionListener(new ExpansionAdapter() {
      override def expansionStateChanged(e: ExpansionEvent) {
        top.setSize(top.computeSize(SWT.DEFAULT, SWT.DEFAULT))
      }
    });
    (content, expand)
  }
  def zproject = viewer.controller.zproject
  def populate(top: Composite, scroll: ScrolledComposite) {
    {
      val lay = new GridLayout
      lay.numColumns = 1
      top.setLayout(lay)
    }
    def createButton(content: Composite, name: String, image: Image) = {
      val b = new Button(content, SWT.PUSH)
      val data = new GridData
      data.horizontalAlignment = SWT.CENTER
      b.setLayoutData(data)
      b.setToolTipText(name)
      b.setImage(image)
      b.setSize(48, 48)
      b
    }
    def portDecl(content: Composite, dir: PortDir, desc: String) {
      val b = createButton(content, "Port " + desc, viewer.imageFactory.portImg(dir)) 
      addOnDispose(b) { b.getImage.dispose() }
      addReaction(b) {
        viewer.tool.state.abort()
        viewer.tool.creatingPort.enter(dir, container)
        viewer.canvas.setFocus()
        hide()
      }
    }
    // PORTS
    val (ports, portsBar) = newBar("Ports", top)
    portDecl(ports, In, "Input port")
    portDecl(ports, Out, "Output port")
    if (container.isInstanceOf[OpenBoxFigure])
      portDecl(ports, Shift, "Shift port")
    portsBar.setExpanded(true)
    // PACKAGES
    val grouped = EclipseUtils.withProgress[Map[String,Seq[BoxTypeProxy]]]("Fetching palette") { monitor ⇒
      zproject.index(monitor).groupBy(proxy ⇒ proxy.name.str.splitAt(proxy.name.str.lastIndexOf("."))._1)
    }
    for ((packName, proxies) ← grouped.toList.sortWith(_._1 < _._1)) {
      val (content, _) = newBar(packName, top)
      for (boxProxy ← proxies.sortBy(_.name.str)) {
        val img = viewer.imageFactory(boxProxy.name)
        val b = createButton(content, boxProxy.name.str, img)
        addOnDispose(b) { img.dispose }
        addReaction(b) {
          viewer.tool.state.abort()
          viewer.tool.creating.enter(boxProxy.name, container)
          viewer.canvas.setFocus()
          hide()
        }
      }
    }
    // properties
    val b = newButton(top, "Properties...")
    addReaction(b) {
      new BoxDefDialog(viewer, viewer.boxDef).open()
    }
  }
}