package net.cp.ac.test.ui;

import net.cp.ac.R;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.DialogPreference;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TimePicker;
import android.widget.TimePicker.OnTimeChangedListener;

/**
 * 
 * @author Jade Yan
 * @since 2012-12-27
 * Customized TimePickerPreference
 */
public class TimePickerPreference extends DialogPreference implements DialogInterface.OnClickListener, DialogInterface.OnDismissListener{

	TimePicker picker;
	int defHour, defMinute; // default values for hour and minute pickers.
	int tmpHour, tmpMinute;	// temple values for hour and minute pickers.
	
	public TimePickerPreference(Context context, AttributeSet attrs, int hourOfDay, int minute) 
	{
		super(context, attrs);
		this.setDialogLayoutResource(R.layout.timepicker);
		this.setDialogTitle(R.string.auto_sync_period_time_note);
		defHour = tmpHour = hourOfDay;
		defMinute = tmpMinute = minute;
	}

	@Override
    protected void onBindDialogView(View view)
	{
		super.onBindDialogView(view);
		picker = (TimePicker)view.findViewById(R.id.timepicker);
		picker.setIs24HourView(true);
		
        picker.setCurrentHour(defHour);
        picker.setCurrentMinute(defMinute);
        
		picker.setOnTimeChangedListener(new OnTimeChangedListener()
		{

			@Override
			public void onTimeChanged(TimePicker view, int hourOfDay, int minute) 
			{
				tmpHour = hourOfDay;
				tmpMinute = minute;
			}
		});
    }
 
	@Override
    protected void onDialogClosed(boolean positiveResult)
	{
		super.onDialogClosed(positiveResult);
		
		//Only if the user click the positive button, we should save the resulted value.
		if(positiveResult)
		{
			/* we have to clear focus first, because if we input an number via soft keyboard,
			 * the edit text in number picker would not save its text we inputed 
			 * unless the focus was removing from it
			 */
			picker.clearFocus();
			defHour = tmpHour;
			defMinute = tmpMinute;
		}
	}
	
	public int getHour()
	{
		return defHour;
	}
	
	public int getMinute()
	{
		return defMinute;
	}
	
}
