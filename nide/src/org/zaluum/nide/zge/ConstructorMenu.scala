package org.zaluum.nide.zge

import org.eclipse.swt.layout.GridLayout
import org.eclipse.jface.dialogs.Dialog
import org.zaluum.nide.compiler._
import org.eclipse.jface.viewers._
import org.eclipse.swt.SWT
import org.eclipse.swt.SWT.NONE
import org.eclipse.swt.graphics.Image
import net.miginfocom.swt.MigLayout
import org.eclipse.swt.widgets.{ Table, TableItem, Shell, Composite, TableColumn, Label, Combo, Control }

class ConstructorMenu(shell: Shell, controller: Controller, vs: ValSymbol) extends Dialog(shell) {
  var combo: ComboViewer = _
  def comboValue = {
    val sel = combo.getSelection.asInstanceOf[IStructuredSelection]
    if (sel.isEmpty) None
    else Some(sel.getFirstElement.asInstanceOf[Constructor])
  }
  var tableContents = List[TableEntry]()
  case class TableEntry(var sym: Option[ParamSymbol], var value: String)

  override protected def okPressed() {
    val typeNames = for (c ← comboValue.toList; p ← c.params) yield p.tpe.name
    val tr = new EditTransformer() {
      val trans: PartialFunction[Tree, Tree] = {
        case v: ValDef if vs.decl == v ⇒
          v.copy(
            constructorParams = tableContents.map(_.value),
            constructorTypes = typeNames)
      }
    }
    super.okPressed();
    controller.exec(tr)
  }
  def createTable(parent: Composite) = {
    val table = new Table(parent, SWT.SINGLE | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL |
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
    val c = new Composite(sup, NONE)
    //applyDialogFont(c);
    c.setLayout(new MigLayout)
    val l = new Label(c, NONE)
    l.setText("Select the constructor to be used")
    combo = new ComboViewer(c)
    combo.setContentProvider(ArrayContentProvider.getInstance)
    val tpe = vs.tpe.asInstanceOf[BoxTypeSymbol]
    combo.setInput(tpe.constructors.toArray)
    combo.getControl.setLayoutData("span,wrap")
    vs.constructor foreach { cons ⇒
      combo.setSelection(new StructuredSelection(cons));
    }
    // TABLE
    tableContents = createTableValue(vs.constructor, vs.decl.asInstanceOf[ValDef].constructorParams)
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
          case 0 ⇒ te.sym map { _.name.str } getOrElse { "?" }
          case 1 ⇒ te.sym map { _.tpe.name.str } getOrElse { "?" }
          case 2 ⇒ te.value
          case _ ⇒ null
        }
      }
    })
    tableViewer.setInput(this)
    // listeners
    combo.addPostSelectionChangedListener(new ISelectionChangedListener {
      def selectionChanged(event: SelectionChangedEvent) {
        tableContents = createTableValue(comboValue, tableContents.map { _.value } filter { _ != "" })
        tableViewer.refresh()
      }
    })
    c
  }
  def createTableValue(c: Option[Constructor], values: List[String]) = {
    val withSymbol = for (cons ← c.toList; p ← cons.params) yield { TableEntry(Some(p), "") }
    val others = for (i ← withSymbol.length until values.length) yield new TableEntry(None, "")
    val table = withSymbol ++ others
    for ((v, t) ← values.zip(table)) t.value = v
    table
  }
  override def isResizable = true
}