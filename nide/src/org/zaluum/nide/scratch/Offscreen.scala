package org.zaluum.nide.scratch

import org.eclipse.swt.events.PaintEvent
import org.eclipse.swt.events.PaintListener
import org.eclipse.swt.graphics.GC
import org.eclipse.swt.graphics.Image
import org.eclipse.swt.widgets.Button
import org.eclipse.swt.widgets.Canvas
import org.eclipse.swt.widgets.Display
import org.eclipse.swt.widgets.Event
import org.eclipse.swt.widgets.Listener
import org.eclipse.swt.widgets.Shell
import org.eclipse.swt.widgets.Table
import org.eclipse.swt.widgets.TableItem
import org.eclipse.swt.SWT

object Snippet95 {

  def main(args: Array[String]) {
    val display = new Display();
    val shellCapture = new Shell(display);
    val shell = new Shell(display, SWT.TOOL);
    shell.setText("Widget");

    val table = new Table(shell, SWT.MULTI);
    table.setLinesVisible(true);
    table.setBounds(10, 10, 100, 100);
    for (i ← 0 to 9) {
      new TableItem(table, SWT.NONE).setText("item" + i);
    }
    val button = new Button(shellCapture, SWT.PUSH);
    button.setText("Capture");
    button.pack();
    button.setLocation(10, 140);
    button.addListener(SWT.Selection, new Listener() {
      def handleEvent(event: Event) {
        val tableSize = table.getSize();
        val bounds = display.getBounds()
        shell.setVisible(true)
        shell.setLocation(bounds.x + bounds.width, 0)
        val gc = new GC(table);
        val image =
          new Image(display, tableSize.x, tableSize.y);
        gc.copyArea(image, 0, 0);
        gc.dispose();
        val popup = new Shell(shell);
        popup.setText("Image");
        popup.addListener(SWT.Close, new Listener() {
          def handleEvent(e: Event) {
            image.dispose();
          }
        });

        val canvas = new Canvas(popup, SWT.NONE);
        canvas.setBounds(10, 10, tableSize.x + 10, tableSize.y + 10);
        canvas.addPaintListener(new PaintListener() {
          def paintControl(e: PaintEvent) {
            e.gc.drawImage(image, 0, 0);
          }
        });
        popup.pack();
        popup.open();
      }
    });
    shell.pack();
    shellCapture.pack()
    shellCapture.open();
    while (!shell.isDisposed()) {
      if (!display.readAndDispatch()) display.sleep();
    }
    display.dispose();
  }
}