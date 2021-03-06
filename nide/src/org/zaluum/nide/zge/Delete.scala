package org.zaluum.nide.zge

import org.zaluum.nide.compiler._

object Delete {
  def deleteSelectionAndPaste(
    selected: Set[Item],
    g: (Block ⇒ Option[ConnectionGraph]),
    relocatedPaste: Option[Clipboard] = None,
    dstPaste: Option[ContainerItem] = None) = {
    val selection = (for (i ← selected; s ← i.selectionSubject) yield s).toList
    val valDefs = selection collect { case v: ValDef ⇒ v }
    val portDefs = selection collect { case p: PortDef ⇒ p }
    val connDefs = selection.collect { case c: ConnectionDef ⇒ c } ++
      selection.collect { case LineSelectionSubject(c, l) ⇒ c }
      def isRemoved(c: ConnectionDef): Boolean = {
          def connectsRemovedVal(o: Option[ConnectionEnd]) = o match {
            case Some(p: PortRef) ⇒
              p.fromRef match {
                case s: SymbolTree[_] ⇒
                  s.sym match {
                    case v: ValSymbol ⇒ valDefs.contains(v.decl.asInstanceOf[ValDef])
                    case _            ⇒ false
                  }
                case _ ⇒ false
              }
            case _ ⇒ false
          }
          def connectsRemovedPortDef(o: Option[ConnectionEnd]) = o match {
            case Some(p: PortRef) ⇒ p.sym.pi.portSymbol match {
              case Some(p) ⇒ selection.contains(p.decl)
              case _       ⇒ false
            }
            case _ ⇒ false
          }
        connectsRemovedPortDef(c.a) || connectsRemovedPortDef(c.b) ||
          connectsRemovedVal(c.a) || connectsRemovedVal(c.b) || connDefs.contains(c)
      }

    new EditTransformer() {
      val trans: PartialFunction[Tree, Tree] = {
        case t: Template ⇒
          val pastePorts =
            if (Some(t) == dstPaste.map(_.template))
              relocatedPaste.toList.flatMap(_.ports)
            else
              List()
          t.copy(blocks = transformTrees(t.blocks),
            ports = transformTrees(t.ports.filterNot { portDefs contains }) ++ pastePorts)
        case b: Block ⇒
          g(b) match {
            case Some(gb) ⇒ // maybe it's not shown so has no graph
              val (pastedVals, pastedCons) = if (Some(b) == dstPaste.map(_.block))
                (relocatedPaste.toList.flatMap(clip ⇒ clip.valDefs),
                  relocatedPaste.toList.flatMap(clip ⇒ clip.connections))
              else (List(), List())
              val removedEdges = for (e ← gb.edges; c ← e.srcCon; if isRemoved(c)) yield e
              val removedg = removedEdges.foldLeft(gb)((gg, e) ⇒ gg.remove(e))
              val (newCons, newJunc) = removedg.prune.clean.toTree
              b.copy(
                valDefs = transformTrees(b.valDefs filterNot { valDefs contains (_) }) ++ pastedVals,
                parameters = transformTrees(b.parameters),
                connections = newCons ++ pastedCons,
                junctions = newJunc)
            case None ⇒ b
          }
      }
    }
  }
}