package org.zaluum.runtime;

@Box
public abstract class LoopBox extends RunnableBox{
  @Out(x=0,y=48) public boolean cond;
  @Override
  public void apply() {
    do{
      contents();
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }while(cond);
  }
}
