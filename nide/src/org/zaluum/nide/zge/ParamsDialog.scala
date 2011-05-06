package org.zaluum.nide.zge

import org.eclipse.ui.ISharedImages
import org.eclipse.ui.PlatformUI
import net.miginfocom.swt.MigLayout
import org.eclipse.jface.dialogs.Dialog
import org.eclipse.jface.viewers._
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.widgets.{ Menu,MenuItem,Table, TableItem, Shell, Composite, TableColumn, Label, Control }
import org.eclipse.swt.SWT
import org.eclipse.swt.events.{ KeyListener, KeyEvent }
import org.zaluum.nide.compiler._
import SWTScala._
// TODO merge table functionality with ConstructorDialog
class ParamsDialog(viewer: Viewer, vs: ValSymbol) extends Dialog(viewer.shell) {
  var tableContents = List[TableEntry]()
  case class TableEntry(var sym: Option[ParamSymbol], var key:String, var value: String)
  def v = vs.decl.asInstanceOf[ValDef]
  def bso = vs.tpe match {
    case NoSymbol => None
    case bs:BoxTypeSymbol => Some(bs)
  }
  override protected def okPressed() {
    val newParams = tableContents.filterNot(_.key=="").map(t => (Param(Name(t.key),t.value))).sortBy(_.key.str)
    val origParams = v.params.asInstanceOf[List[Param]].sortBy(_.key.str)
    if (newParams != origParams){ 
      val tr = new EditTransformer() {
        val trans: PartialFunction[Tree, Tree] = {
          case v: ValDef if vs.decl == v ⇒
            v.copy(params = newParams)
        }
      }
      super.okPressed()
      viewer.controller.exec(tr)
    } else {
      super.okPressed()
    }
  }
  def createTable(parent: Composite) = {
    val table = new Table(parent, SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL |
      SWT.FULL_SELECTION | SWT.HIDE_SELECTION)
    val nameCol = new TableColumn(table, SWT.RIGHT)
    table.setLinesVisible(true);
    table.setHeaderVisible(true);
    def newColumn(str: String, w: Int) {
      val col = new TableColumn(table, SWT.CENTER, 0);
      col.setText(str);
      col.setWidth(w);
    }
    newColumn("Name", 150)
    newColumn("Type", 150)
    newColumn("Value", 150)
    table
  }
  class Loader(loader: ⇒ Iterable[AnyRef]) extends IStructuredContentProvider {
    def dispose() {}
    def getElements(inputElement: AnyRef): Array[AnyRef] = loader.toArray
    def inputChanged(viewer: org.eclipse.jface.viewers.Viewer, oldInput: Any, newInput: Any) {}
  }
  override def createDialogArea(parent: Composite): Control = {
    val sup = super.createDialogArea(parent).asInstanceOf[Composite];
    val c = new Composite(sup, SWT.NONE)
    c.setLayout(new MigLayout)
    newLabel(c,"Parameter values",layout="wrap")
    // TABLE
    tableContents = createTableValue()
    val table = createTable(c)
    table.setLayoutData("span,  height 200")
    val tableViewer = new TableViewer(table)
    val columnNames = Array("Name", "Type", "Value")
    tableViewer.setColumnProperties(columnNames);
    tableViewer.setUseHashlookup(true)
    tableViewer.setContentProvider(new Loader(tableContents))
    val editors: Array[CellEditor] = Array.ofDim(columnNames.length)
    editors(0) = null
    editors(1) = null
    editors(2) = new TextCellEditor(table)
    tableViewer.setCellEditors(editors)
    tableViewer.setCellModifier(new ICellModifier {
      def canModify(element: AnyRef, property: String) = property == "Value"
      def toTE(a: AnyRef) = a.asInstanceOf[TableItem].getData.asInstanceOf[TableEntry]
      def getValue(element: AnyRef, property: String) = element.asInstanceOf[TableEntry].value
      def modify(element: AnyRef, property: String, value: AnyRef) {
        toTE(element).value = value.asInstanceOf[String]
        tableViewer.refresh()
      }
    })
    tableViewer.setLabelProvider(new LabelProvider with ITableLabelProvider {
      def getColumnImage(element: AnyRef, columnIndex: Int): Image = null
      def getColumnText(element: AnyRef, columnIndex: Int) = {
        val te = element.asInstanceOf[TableEntry]
        columnIndex match {
          case 0 ⇒ te.key
          case 1 ⇒ te.sym map { _.tpe.name.str } getOrElse { "?" }
          case 2 ⇒ te.value
          case _ ⇒ null
        }
      }
    })
    tableViewer.setInput(this)
    // listeners
    // DELETE
    def delete() {
      val sel = tableViewer.getSelection.asInstanceOf[StructuredSelection]
      if (!sel.isEmpty) {
        import scala.collection.JavaConversions._
        val selected = sel.toList.toList.asInstanceOf[List[TableEntry]]
        val toDelete = selected filter { _.sym.isEmpty }
        if (!toDelete.isEmpty) {
          tableContents = tableContents filterNot (toDelete.contains(_))
          tableViewer.refresh()
        }
      }
    }
    newPopupMenu(getShell,table) { m=>
      newMenuItem(m, "Delete", Some(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ELCL_REMOVE))) {
        delete()
      }
    }
    addKeyReleasedReaction(table,SWT.DEL) {delete()}
    c
  }

  def createTableValue() = {
    val declared = v.params.asInstanceOf[List[Param]]
    def declaredValue(k:Name) =declared.find(_.key == k).map(_.value).getOrElse("")
    val parSymbols = for (bs ← bso.toList; p ← bs.params) yield p
    val withSymbol = for (p<-parSymbols; val key = p.name) yield { TableEntry(Some(p), key.str,declaredValue(key) ) }
    val unknown = for (Param(k,v) ← declared; if !parSymbols.exists(_.name==k)) yield { TableEntry(None, k.str, v) }
    withSymbol ++ unknown
  }
  override def isResizable = true
}