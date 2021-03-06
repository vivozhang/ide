package org.zaluum.nide.eclipse.integration

import org.codehaus.jdt.groovy.integration.EventHandler
import org.eclipse.jdt.internal.core.JavaProject

class ZaluumEventHandler extends EventHandler {
  def handle(javaProject: JavaProject, event: String) {
    if (event == "cleanOutputFolders") {
      if (javaProject != null) {
        //GroovyParser.tidyCache(javaProject.getProject().getName());
      }
    } else if (event == "close") {
      if (javaProject != null) {
        val projectName = javaProject.getProject().getName();
        //GroovyParser.closeClassLoader(projectName);
        // GroovyParser.tidyCache(projectName);
      }
    }
  }
}