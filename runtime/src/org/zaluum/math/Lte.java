package org.zaluum.math;

import org.zaluum.runtime.Box;

@Box
public class Lte extends DoubleBoolOp2{
  public void apply(){
    o = a<=b;
  }
}
