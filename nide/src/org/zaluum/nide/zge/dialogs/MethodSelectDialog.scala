package org.zaluum.nide.zge.dialogs

import java.util.Comparator
import scala.collection.JavaConversions
import org.eclipse.core.runtime.Status
import org.eclipse.jdt.internal.compiler.ast.ASTNode
import org.eclipse.jdt.internal.compiler.lookup.Binding
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding
import org.eclipse.jdt.internal.core.JavaProject
import org.eclipse.jface.dialogs.DialogSettings
import org.eclipse.jface.dialogs.IDialogSettings
import org.eclipse.jface.viewers.LabelProvider
import org.eclipse.swt.widgets.Composite
import org.zaluum.nide.compiler.BoxTypeSymbol
import org.zaluum.nide.compiler.ClassJavaType
import org.zaluum.nide.compiler.EditTransformer
import org.zaluum.nide.compiler.Param
import org.zaluum.nide.compiler.Tree
import org.zaluum.nide.compiler.ValDef
import org.zaluum.nide.compiler.ValSymbol
import org.zaluum.nide.compiler.ZaluumCompletionEngineScala
import org.zaluum.nide.eclipse.integration.model.MethodUtils
import org.zaluum.nide.zge.Viewer
import org.zaluum.nide.compiler.SignatureExprType
import org.zaluum.nide.compiler.StaticExprType
import org.zaluum.nide.compiler.InvokeStaticExprType
import org.zaluum.nide.compiler.InvokeExprType
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding

class MethodSelectDialog(viewer: Viewer, val vs: ValSymbol) extends FilteredItemsSelectionDialog2(viewer.shell, false) {
  override def isResizable = true
  override protected def okPressed() {
    execCommand()
    super.okPressed()
  }
  def execCommand() {
    getSelectedItems().getFirstElement() match {
      case m: MethodWithNames ⇒
        val tpe = vs.tpe.asInstanceOf[SignatureExprType]
        val tr = vs.tdecl.addOrReplaceParam(Param(tpe.signatureName, m.methodSignature))
        viewer.controller.exec(tr)
    }
  }
  object MethodLabelProvider extends LabelProvider {
    override def getText(element: Object) = {
      element match {
        case s: MethodWithNames ⇒ s.text
        case _                  ⇒ null
      }
    }
  }
  object MethodDetailsLabelProvider extends LabelProvider {
    override def getText(element: Object) = {
      element match {
        case s: MethodWithNames ⇒ s.fullText
        case _                  ⇒ null
      }
    }
  }

  val id = "org.zaluum.nide.methodSelectDialog"
  val settings = new DialogSettings(id);
  val static = vs.tpe.isInstanceOf[StaticExprType]
  val binding = vs.tpe match {
    case InvokeExprType ⇒ InvokeExprType.thisPort(vs).finalTpe.binding
    case InvokeStaticExprType ⇒
      vs.classinfo match {
        case cl: ClassJavaType ⇒ cl.binding
        case _                 ⇒ null
      }
  }
  val items: Array[MethodWithNames] = binding match {
    case r: ReferenceBinding ⇒
      val engine = ZaluumCompletionEngineScala.engineForVs(vs)
      val scope = vs.owner.template.asInstanceOf[BoxTypeSymbol].javaScope; // FIXME?
      val methods = ZaluumCompletionEngineScala.allMethods(engine, scope, r, static) // FIXME
      val jproject = viewer.zproject.jProject.asInstanceOf[JavaProject]
      val nameLookup = jproject.newNameLookup(Array[org.eclipse.jdt.core.ICompilationUnit]())
      val paramNames = methods map { m ⇒
        val names = MethodUtils.findMethodParamNames(m, jproject)
        val params = names.toList.flatMap(a ⇒ a)
        MethodWithNames(m, params)
      }
      paramNames.sortBy(_.selector).toArray
    case _ ⇒ Array()
  }
  val currentMethodSig = vs.params.values.headOption
  val currentMethod = currentMethodSig flatMap { mstr ⇒
    items.find { _.methodSignature == mstr }
  }
  setTitle("Select method");
  setMessage("Choose method to invoke" +
    (if (currentMethod.isEmpty)
      " - current method signature: " + currentMethodSig.getOrElse("<missing>")
    else ""))
  setInitialPattern("**");
  setListLabelProvider(MethodLabelProvider);
  setDetailsLabelProvider(MethodDetailsLabelProvider);
  setInitialElementSelections(JavaConversions.seqAsJavaList(currentMethod.toList))
  class MethodItemsFilter extends ItemsFilter {
    if (this.getPattern() == null || this.getPattern == "") patternMatcher.setPattern("**")

    def isConsistentItem(item: AnyRef) = item.isInstanceOf[MethodWithNames]
    def matchItem(item: Object) = item match {
      case m: MethodWithNames ⇒ matches(m.text)
      case _                  ⇒ false
    }
  }

  lazy val valuesToFill: java.lang.Iterable[_] = JavaConversions.asJavaIterable(items.toIterable)

  protected def getDialogSettings(): IDialogSettings = settings
  protected def getItemsComparator() = new Comparator[MethodWithNames]() {
    def compare(m1: MethodWithNames, m2: MethodWithNames): Int = m1.text.compareTo(m2.text)
  }
  protected def validateItem(item: Object) = Status.OK_STATUS
  protected def createExtendedContentArea(parent: Composite) = null
  protected def createFilter = new MethodItemsFilter()
  def getElementName(item: Object) = item match {
    case m: MethodWithNames ⇒ m.text
    case _                  ⇒ null
  }
}

case class MethodWithNames(m: MethodBinding, paramNames: List[String]) {
  def flags = {
    val s = new StringBuffer()
    ASTNode.printModifiers(m.modifiers, s);
    s.toString
  }
  def returnStr = if (m.returnType != null) m.returnType.debugName() else "<no type>"
  def params = {
    if (m.parameters != null) {
      "(" +
        (if (m.parameters != Binding.NO_PARAMETERS) {
          val padded = paramNames.padTo(m.parameters.length, "?")
          val zip = padded.zip(m.parameters)
          zip.map {
            case (name, p) ⇒
              if (p != null) p.debugName() + " " + name else "<no argument type>"
          } mkString (", ")
        } else "") + ")"
    } else {
      "<no argument types>"
    }
  }
  def selector = m.selector.mkString
  def declaringClass = new String(m.declaringClass.readableName())
  def methodSignature = MethodUtils.toMethodSig(m)
  def fullText = MethodUtils.toMethodStr(m, paramNames) + " - " + declaringClass
  def text = selector + " " + params + " : " + returnStr + " - " + declaringClass
}