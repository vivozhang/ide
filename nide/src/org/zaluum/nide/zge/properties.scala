package org.zaluum.nide.zge
import scala.Array.canBuildFrom

import org.eclipse.jdt.core.search.IJavaSearchConstants
import org.eclipse.jdt.core.search.SearchEngine
import org.eclipse.jdt.core.IJavaElement
import org.eclipse.jdt.core.IJavaProject
import org.eclipse.jdt.core.IType
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding
import org.eclipse.jdt.internal.core.JavaProject
import org.eclipse.jdt.internal.ui.dialogs.OpenTypeSelectionDialog
import org.eclipse.jface.viewers.ICellEditorValidator
import org.eclipse.jface.viewers.LabelProvider
import org.eclipse.jface.window.Window
import org.eclipse.swt.widgets.Control
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Shell
import org.eclipse.ui.dialogs.FilteredItemsSelectionDialog
import org.eclipse.ui.views.properties.ComboBoxPropertyDescriptor
import org.eclipse.ui.views.properties.IPropertyDescriptor
import org.eclipse.ui.views.properties.IPropertySource2
import org.eclipse.ui.views.properties.PropertyDescriptor
import org.eclipse.ui.views.properties.TextPropertyDescriptor
import org.eclipse.ui.PlatformUI
import org.zaluum.nide.compiler.BeanParamDecl
import org.zaluum.nide.compiler.BoxDef
import org.zaluum.nide.compiler.BoxExprType
import org.zaluum.nide.compiler.Expressions
import org.zaluum.nide.compiler.JavaType
import org.zaluum.nide.compiler.ClassJavaType
import org.zaluum.nide.compiler.Name
import org.zaluum.nide.compiler.Param
import org.zaluum.nide.compiler.ParamDecl
import org.zaluum.nide.compiler.PortDef
import org.zaluum.nide.compiler.Signatures
import org.zaluum.nide.compiler.ValDef
import org.zaluum.nide.compiler.Values
import org.zaluum.nide.compiler.VarDecl
import org.zaluum.nide.eclipse.integration.model.ZaluumClassScope
import org.zaluum.nide.eclipse.integration.model.ZaluumCompletionEngine
import org.zaluum.nide.utils.SWTScala
import org.zaluum.nide.zge.dialogs.ConstructorSelectDialog
import org.zaluum.nide.zge.dialogs.FieldSelectDialog
import org.zaluum.nide.zge.dialogs.MethodSelectDialog

trait Property {
  def descriptor: IPropertyDescriptor
  def set(value: AnyRef)
  def get: AnyRef
  def isSet: Boolean
  def canBeReset = true
  def reset()
}
trait ControllerProperty extends Property {
  def c: Controller
  def displayName: String
}
trait ParamProperty extends ControllerProperty {
  def p: ParamDecl
  def v: ValDef
  def key: Name = p.name
  def displayName = p.name.str
  def currentVal: Option[String] = v.sym.getStr(p)
  def set(value: AnyRef) = {
    c.exec(
      if (value == "") v.removeParams(key)
      else v.addOrReplaceParam(Param(key, value.toString)))
  }
  def get: AnyRef = v.params.find(_.key == key).map { _.valueStr } getOrElse ("")
  def isSet = v.params.exists(_.key == key)
  def reset() { c.exec(v.removeParams(p.name)) }

}
class PortNameProperty(p: PortDef, val c: Controller) extends ControllerProperty with NoResetProperty {
  def descriptor = new TextPropertyDescriptor(this, displayName)
  def displayName = "Name"
  def currentVal = Some(get)
  def set(value: AnyRef) = if (value != p.name.str) c.exec(p.renamePort(value.toString, None))
  def get = p.name.str
  def isSet: Boolean = true
}
class PortTypeProperty(p: PortDef, val c: Controller) extends TypeProperty with NoResetProperty {
  def displayName = "Type"
  def currentVal = Some(get)
  def set(value: AnyRef) =
    if (value != p.typeName.str) c.exec(p.changeType(value.toString()))
  def get = p.typeName.str
  def isSet: Boolean = true
}
trait NoResetProperty extends Property {
  override def canBeReset = false
  def reset() {}
}
abstract class InitProperty(b: BoxDef, val c: Controller) extends Property {
  def scope = b.sym.scope
  def className = split.map { _._1 }
  def methodUID = split.map { _._2 }
  def split = b.initMethod.map { Signatures.staticMethodUIDSplit }
  def tpe = className.flatMap { c ⇒ scope.lookupType(Name(c)) }
  def set(cl: String, m: String) = {
    val comm = if (cl == "" && m == "") b.editInitMethod(None)
    else b.editInitMethod(Some(cl + "#" + m))
    c.exec(comm)
  }
  def isSet: Boolean = b.initMethod.isDefined
  def reset() { set("", "") }
}
class InitMethodProperty(b: BoxDef, c: Controller) extends InitProperty(b, c) with MethodProperty {
  val displayName = "*Init method"
  def currentVal = methodUID
  def set(value: AnyRef) {
    set(className.getOrElse(""), value.toString)
  }
  def get: AnyRef = methodUID.getOrElse("")
  def findMethods(c: ClassJavaType) =
    c.allMethods(true, scope)
}
class InitMethodClassProperty(b: BoxDef, c: Controller) extends InitProperty(b, c) with TypeProperty {
  val displayName = "*Init method class"
  def currentVal = className.orElse(b.initMethod)
  def set(value: AnyRef) {
    set(value.toString, methodUID.getOrElse(""))
  }
  def get: AnyRef = className.orElse(b.initMethod).getOrElse("")
}
object ConstructorDeclProperty {
  lazy val Regexp = """\s*(\S+)\s*(\S+)\s*""".r
}
class ConstructorDeclProperty(boxDef: BoxDef, val c: Controller) extends Property {
  val descriptor = new TextPropertyDescriptor(this, "*Constructor declaration")
  descriptor.setValidator(new ICellEditorValidator() {
    def isValid(value: AnyRef) = {
      parse(value.toString) match {
        case Left(msg) ⇒ msg
        case _         ⇒ null
      }
    }
  })
  def parse(value: String): Either[String, List[VarDecl]] = {
    val params = value.toString.split(",").map(_.trim)
    Right(
      for (p ← params.toList) yield {
        p match {
          case ConstructorDeclProperty.Regexp(tpe, name) ⇒
            VarDecl(Name(name), Name(tpe))
          case _ ⇒
            return Left("Cannot parse parameter " + p + ". Format is \"Type Name, Type Name, ...\"");
        }
      })
  }
  def set(value: AnyRef) {
    parse(value.toString) match {
      case Left(_)  ⇒
      case Right(l) ⇒ c.exec(boxDef.editConstructor(l))
    }
  }
  // FIXME BUG updated twice because set value might not be equal to this (white space)
  // Use Value system?
  def get: AnyRef = boxDef.constructor.map { c ⇒ c.tpeName.str + " " + c.name.str }.mkString(", ")
  def isSet: Boolean = !boxDef.constructor.isEmpty
  def reset() { set("") }
}

class ValDefTypeProperty(v: ValDef, c: Controller) extends NoResetProperty {
  lazy val vals = Expressions.all.keys.map(_.str).toArray.sorted
  def descriptor = new ComboBoxPropertyDescriptor(this, "*Type", vals)
  def set(value: AnyRef) {
    val i = value.asInstanceOf[Int]
    c.exec(v.changeType(vals(i)))
  }
  def get = vals.indexWhere(_ == v.typeName.str).asInstanceOf[AnyRef]
  def isSet: Boolean = true
}
class NameProperty(valDef: ValDef, controller: Controller) extends NoResetProperty {
  def descriptor = new TextPropertyDescriptor(this, "*Name")
  def set(value: AnyRef) {
    if (value != get && value != null && value != "")
      controller.exec(valDef.sym.owner.rename(valDef, Name(value.toString)))
  }
  def get = valDef.name.str
  def isSet = true
}
class LabelProperty(valDef: ValDef, controller: Controller, gui: Boolean) extends Property {
  def lbl = if (gui) valDef.labelGui else valDef.label
  def txt = if (gui) "*Label GUI" else "*Label"
  def descriptor: IPropertyDescriptor = new TextPropertyDescriptor(this, txt)
  def set(value: AnyRef) = controller.exec(valDef.editLabel(gui, value.toString))
  def get: AnyRef = lbl.map { _.description }.getOrElse("")
  def isSet: Boolean = lbl.isDefined
  def reset() = set("")
}

class ConstructorSelectProperty(valDef: ValDef, controller: Controller) extends Property {
  def descriptor = new DialogPropertyDescriptor(this, "#Constructor") {
    override lazy val labelProvider = new LabelProvider() {
      override def getText(element: AnyRef) = {
        element match {
          case (sig: String, params: List[_]) ⇒ params.mkString(", ")
          case _                              ⇒ ""
        }
      }
    }
    def openDialog(cell: Control, ignore: AnyRef): String = {
      val c = new ConstructorSelectDialog(controller.zproject.jProject.asInstanceOf[JavaProject],
        cell.getShell, valDef.sym)
      c.open()
      c.result foreach { comm ⇒
        SWTScala.async(cell.getDisplay) { controller.exec(comm) }
      }
      null
    }
  }
  def set(value: AnyRef) {} // done in dialog
  val sig = valDef.sym.getStr(BoxExprType.constructorTypesDecl)
  val params = valDef.sym.getList(BoxExprType.constructorParamsDecl)
  def get = (sig.getOrElse(""), params.getOrElse(List()))
  def isSet: Boolean = sig.isDefined && params.isDefined
  def reset() = controller.exec(
    valDef.removeParams(BoxExprType.constructorParamsDecl.fqName, BoxExprType.constructorTypesDecl.fqName))
}
class MissingParamProperty(controller: Controller, p: Param, v: ValDef) extends Property {
  def descriptor = new TextPropertyDescriptor(this, "<" + p.key.str + ">")
  def set(value: AnyRef) {
    if (value == "")
      controller.exec(v.removeParams(p.key))
    else
      controller.exec(v.addOrReplaceParam(Param(p.key, value.toString)))
  }
  def get: AnyRef = p.valueStr
  def isSet = true
  def reset = set("")
}

class TextParamProperty(val c: Controller, val p: ParamDecl, val v: ValDef)
    extends ParamProperty {
  def descriptor: PropertyDescriptor = new TextPropertyDescriptor(this, p.name.str)
}
class MultiLineTextParamProperty(val c: Controller, val p: ParamDecl, val v: ValDef) extends ParamProperty {
  def descriptor = new MultilineTextDescriptor(this, p.name.str)
}
class TextListParamProperty(c: Controller, p: ParamDecl, v: ValDef)
    extends TextParamProperty(c, p, v) {
  override def set(value: AnyRef) = {
    val l = value.toString.split(",").map(_.trim).filter(_ != "").toList
    c.exec(
      if (l.isEmpty) v.removeParams(key)
      else v.addOrReplaceParam(Param(key, l)))
  }
}
class ConstructorParamProperty(
    c: Controller,
    p: ParamDecl,
    v: ValDef,
    ttpe: ⇒ Option[JavaType]) extends TextParamProperty(c, p, v) with MethodProperty {
  def scope = v.sym.mainBS.scope
  def tpe = ttpe
  def findMethods(c: ClassJavaType) =
    c.allConstructors(scope)
}
trait MethodProperty extends ControllerProperty {
  def tpe: Option[JavaType]
  def classJavaType = tpe match {
    case Some(c: ClassJavaType) ⇒ Some(c)
    case None                   ⇒ None
  }
  def scope: ZaluumClassScope
  def currentVal: Option[String]
  def findMethods(c: ClassJavaType): List[MethodBinding]
  override def descriptor = new TextDialogPropertyDescriptor(this, displayName) {
    def openDialog(cell: Control, a: AnyRef) = {
      val m = new MethodSelectDialog(
        c.zproject.jProject.asInstanceOf[JavaProject],
        cell.getShell(),
        classJavaType,
        scope,
        currentVal) {
        def findMethods() =
          classJavaType map { MethodProperty.this.findMethods } getOrElse (List())
      }
      m.openRet().getOrElse(null)
    }
  }
}

class MethodParamProperty(
    c: Controller,
    p: ParamDecl,
    v: ValDef,
    javatpe: ⇒ Option[JavaType],
    val static: Boolean) extends TextParamProperty(c, p, v) with MethodProperty {
  def scope = v.sym.mainBS.scope
  def tpe = javatpe
  def findMethods(c: ClassJavaType) =
    c.allMethods(static, scope)
}

class FieldParamProperty(
    c: Controller,
    p: ParamDecl,
    v: ValDef,
    tpe: ⇒ Option[JavaType],
    static: Boolean) extends TextParamProperty(c, p, v) {
  override def descriptor = new TextDialogPropertyDescriptor(this, p.name.str) {
    def openDialog(cell: Control, a: AnyRef) = new FieldSelectDialog(
      cell.getShell, tpe, static, v.sym, currentVal).openRet().getOrElse(null)
  }
}

trait TypeProperty extends ControllerProperty {
  def currentVal: Option[String]
  override def descriptor = new TextDialogPropertyDescriptor(this, displayName) {
    def openDialog(cell: Control, a: AnyRef) =
      OpenSearch.openSearch(c.zproject.jProject, cell.getShell, currentVal).getOrElse(null)
  }
}
class TypeParamProperty(
  c: Controller,
  p: ParamDecl,
  v: ValDef) extends TextParamProperty(c, p, v) with TypeProperty

object OpenSearch {
  def openSearch(project: IJavaProject, shell: Shell, initial: Option[String]) = {
    val scope = SearchEngine.createJavaSearchScope(Array[IJavaElement](project))
    val o = new OpenTypeSelectionDialog(shell, false, PlatformUI.getWorkbench().getProgressService(), scope, IJavaSearchConstants.TYPE)
    initial foreach { s ⇒
      o.setInitialPattern(s, FilteredItemsSelectionDialog.FULL_SELECTION)
    }
    if (o.open() == Window.OK) {
      val result = if (o.getResult == null) None else o.getResult.headOption
      result.map { _.asInstanceOf[IType].getFullyQualifiedName() }
    } else None

  }
}
class BeanProperty(
    val c: Controller,
    val v: ValDef,
    val p: BeanParamDecl) extends ParamProperty {
  lazy val tpe = Values.typeFor(p)
  def descriptor: IPropertyDescriptor = tpe.editor(this, p.name.str)
  override def set(value: AnyRef) {
    if (get == value) return
    val encoded = tpe.parseSWT(value)
    super.set(encoded)
  }
  override def get: AnyRef = v.sym.getValue(p) match {
    case Some(v) ⇒ v.toSWT
    case None    ⇒ tpe.defaultSWT
  }
}
trait PropertySource extends IPropertySource2 {
  def display: Display
  var properties = List[Property]()
  def isPropertyResettable(id: AnyRef) = id.asInstanceOf[Property].canBeReset
  def isPropertySet(id: AnyRef) = id.asInstanceOf[Property].isSet
  def getEditableValue() = this
  def getPropertyDescriptors() = { properties.map { _.descriptor }.toArray }
  def getPropertyValue(id: AnyRef): AnyRef = id.asInstanceOf[Property].get
  def resetPropertyValue(id: AnyRef) = id.asInstanceOf[Property].reset()
  def setPropertyValue(id: AnyRef, swtValue: AnyRef) = SWTScala.async(display) {
    val prop = id.asInstanceOf[Property]
    if (prop.get != swtValue)
      prop.set(swtValue)
  }
}