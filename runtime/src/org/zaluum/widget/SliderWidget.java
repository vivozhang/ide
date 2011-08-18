package org.zaluum.widget;

import javax.swing.JSlider;

import org.zaluum.annotation.Apply;
import org.zaluum.annotation.Box;
import org.zaluum.annotation.Out;
/**
 * Hola
 * @author frede
 *
 */
@Box
public class SliderWidget {
	@Out public double out = 0.0;
	public JSlider _widget = new JSlider();
	public SliderWidget(int min,int max) {
		_widget.setMaximum(max);
		_widget.setMinimum(min);
		_widget.setValue(0);
	}
	@Apply
	public void apply() {
		out=_widget.getValue();
	}
}
