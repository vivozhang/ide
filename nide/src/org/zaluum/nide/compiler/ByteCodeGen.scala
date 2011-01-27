package org.zaluum.nide.compiler

import org.zaluum.nide.model.BoxClassName
import org.objectweb.asm._
import Opcodes._
import org.zaluum.nide.model.{ Box, PortRef, ModelPortRef, BoxPortRef, Connection }

object ByteCodeGen {
  def internal(className: String) = className.replace('.', '/')
  def classDescriptor(className: String) = 'L' + internal(className) + ";"
  def className(b:Box) = b.boxClassName.toString // FIXME
  implicit def boxClassName2String (b:BoxClassName) = b.toString
  def dump(c: Compiled): Array[Byte] = {
    val cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    cw.visit(V1_5, ACC_PUBLIC + ACC_SUPER, internal(c.bcd.className), null, "java/lang/Object", null);
    //val av = cw.visitAnnotation("org/zaluum/nide/java/Box", true);
    //av.visitEnd();
    cw.visitSource(c.source, null);
    // FIELDS 
    {
      for (b ← c.boxesInOrder) {
        val fv = cw.visitField(ACC_PUBLIC, b.name, classDescriptor(className(b)), null, null)
        fv.visitEnd()
      }
      for (p ← c.portDeclInOrder) {
        val fv = cw.visitField(ACC_PUBLIC, p.name, p.descriptor , null, null)
        fv.visitEnd
      }
      WidgetTemplateDump.createWidgetField(cw)
      // generate init
      val mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
      mv.visitCode();
      val l0 = new Label();
      mv.visitLabel(l0);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");

      // INIT 
      for (b ← c.boxesInOrder) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitTypeInsn(NEW, internal(className(b)));
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, internal(className(b)), "<init>", "()V");
        mv.visitFieldInsn(PUTFIELD, internal(c.bcd.className), b.name, classDescriptor(className(b)));
      }
      for (p ← c.portDeclInOrder) {
        // TODO init values
      }
      // Create widget
      WidgetTemplateDump.attachInitCode(mv,c)
      
      mv.visitInsn(RETURN);
      val l5 = new Label();
      mv.visitLabel(l5);
      mv.visitLocalVariable("this", classDescriptor(c.bcd.className), null, l0, l5, 0);
      mv.visitMaxs(-1, -1); // autocompute
      mv.visitEnd();
    }
    // METHOD APPLY
    {
      // run all boxes
      val mv = cw.visitMethod(ACC_PUBLIC, "apply", "()V", null, null);
      mv.visitCode();
      val l0 = new Label();
      mv.visitLabel(l0);

      // Utility methods
      def loadBox(b: Box): Unit = {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, internal(c.bcd.className), b.name, classDescriptor(className(b)));
      }
      def getField(className: String, name: String, desc:String, scala: Boolean) {
        if (scala) mv.visitMethodInsn(INVOKEVIRTUAL, internal(className), name, "()"+desc);
        else mv.visitFieldInsn(GETFIELD, internal(className), name, desc);
      }
      def putField(className: String, name: String, desc: String, scala: Boolean) {
        if (scala) mv.visitMethodInsn(INVOKEVIRTUAL, internal(className), name + "_$eq", "("+desc+")V");
        else mv.visitFieldInsn(PUTFIELD, internal(className), name, desc);
      }
      def putRef(p: PortRef, get: ⇒ Unit) = p match {
        case b: BoxPortRef ⇒
          loadBox(b.box)
          get
          
          putField(className(b.box), b.name, c.portType(b).descriptor,c.boxType(b.box).scala)
        case m: ModelPortRef ⇒
          mv.visitVarInsn(ALOAD, 0)
          get
          putField(c.bcd.className, m.name,c.portType(m).descriptor, false)
      }
      def getRef(p: PortRef) = p match {
        case b: BoxPortRef ⇒
          loadBox(b.box)
          getField(className(b.box), b.name, c.portType(b).descriptor,c.boxType(b.box).scala)
        case m: ModelPortRef ⇒
          mv.visitVarInsn(ALOAD, 0)
          getField(c.bcd.className, m.name, c.portType(m).descriptor, false)
      }
      def executeConnection(con: Connection) {
        val (from,to) = con.connectionFlow(c.portType).get
        putRef(to, getRef(from))
      }
      def connectionsFrom(f: PortRef) = {
        c.bcd.connections filter { _.from == Some(f) }
      }
      def connectionsFromBox(b: Box) = {
        c.bcd.connections filter { conn ⇒
          (conn.from, conn.to) match {
            case (Some(f: BoxPortRef), Some(t)) ⇒ f.box == b
            case _ ⇒ false
          }
        }
      }
      // propagate inputs
      for (portDecl ← c.portDeclInOrder) {
        val ref = ModelPortRef(portDecl.name)
        if (c.portType(ref).in) connectionsFrom(ref) foreach { executeConnection(_) }
      }
      for (box ← c.order) {
        // invoke box
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, internal(c.bcd.className), box.name, classDescriptor(className(box)));
        mv.visitMethodInsn(INVOKEVIRTUAL, internal(className(box)), "apply", "()V");
        // propagate
        val connections = connectionsFromBox(box)
        connections foreach { executeConnection(_) }
      }
      val lend = new Label();
      mv.visitInsn(RETURN);
      mv.visitLabel(lend);
      mv.visitLocalVariable("this", classDescriptor(c.bcd.className), null, l0, lend, 0);
      mv.visitMaxs(-1, -1);
      mv.visitEnd();
    }
    
    cw.visitEnd();
    cw.toByteArray();
  }

}
